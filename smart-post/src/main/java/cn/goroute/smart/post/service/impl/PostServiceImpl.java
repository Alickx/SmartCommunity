package cn.goroute.smart.post.service.impl;


import cn.goroute.smart.common.constant.PostConstant;
import cn.goroute.smart.common.constant.RedisKeyConstant;
import cn.goroute.smart.common.dao.*;
import cn.goroute.smart.common.entity.dto.MemberDto;
import cn.goroute.smart.common.entity.dto.PostDto;
import cn.goroute.smart.common.entity.dto.PostListDto;
import cn.goroute.smart.common.entity.pojo.Category;
import cn.goroute.smart.common.entity.pojo.Post;
import cn.goroute.smart.common.entity.pojo.PostTag;
import cn.goroute.smart.common.entity.pojo.Tag;
import cn.goroute.smart.common.entity.vo.PostQueryVo;
import cn.goroute.smart.common.entity.vo.PostVo;
import cn.goroute.smart.common.feign.MemberFeignService;
import cn.goroute.smart.common.service.AuthService;
import cn.goroute.smart.common.utils.*;
import cn.goroute.smart.post.feign.SearchFeignService;
import cn.goroute.smart.post.manage.IPostManage;
import cn.goroute.smart.post.service.PostService;
import cn.goroute.smart.post.util.Html2TextUtil;
import cn.goroute.smart.post.util.NamingThreadFactory;
import cn.goroute.smart.post.util.RabbitmqUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Service("postService")
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostDao, Post> implements PostService {

    @Resource
    MemberFeignService memberFeignService;

    @Resource
    SearchFeignService searchFeignService;

    @Autowired
    RabbitmqUtil rabbitmqUtil;

    @Autowired
    TagDao tagDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    PostDao postDao;

    @Autowired
    PostTagDao postTagDao;

    @Autowired
    CommentDao commentDao;

    @Autowired
    RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    CollectDao collectDao;

    @Autowired
    IPostManage iPostManage;

    @Autowired
    AuthService authService;

    private static final int CORE_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * ??????????????????
     *
     * @param postQueryVO ????????????
     * @return ??????????????????
     */
    @Override
    public Result queryPage(PostQueryVo postQueryVO) {

        IPage<Post> page;

        // ???????????? ??????????????????????????????????????????
        if (postQueryVO.getCategoryUid() == null) {
            page = postDao.selectPage(
                    new Query<Post>().getPage(postQueryVO),
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getIsPublish, PostConstant.PUBLISH)
                            .eq(Post::getStatus, PostConstant.NORMAL_STATUS));
            // ????????????????????????????????????????????????????????????????????????
        } else if (postQueryVO.getTagUid() == null) {

            page = postDao.selectPage(
                    new Query<Post>().getPage(postQueryVO),
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getCategoryUid, postQueryVO.getCategoryUid())
                            .eq(Post::getStatus, PostConstant.NORMAL_STATUS)
            );
        } else {
            // ?????????????????????????????????????????????????????????????????????
            IPage<PostTag> postTagIPage = postTagDao.selectPage(new Query<PostTag>().getPage(postQueryVO),
                    new LambdaQueryWrapper<PostTag>()
                            .eq(PostTag::getTagUid, postQueryVO.getTagUid()));

            List<PostTag> records = postTagIPage.getRecords();
            if (CollUtil.isEmpty(records)) {
                return Result.ok().put("data", new Page<>());
            }
            List<Long> postIds = records.stream()
                    .map(PostTag::getPostUid).collect(Collectors.toList());

            List<Post> posts = postDao.selectBatchIds(postIds);
            BeanUtils.copyProperties(postTagIPage, page = new Page<>());
            page.setRecords(posts);
        }

        boolean isLogin = authService.getIsLogin();

        List<Post> records = page.getRecords();

        List<PostListDto> postList = getPostListDTOS(isLogin, records);

        PageUtils pageUtils = new PageUtils(page);

        pageUtils.setList(postList);

        return Result.ok().put("data", pageUtils);
    }


    /**
     * ????????????????????????
     *
     * @param isLogin ??????????????????
     * @param records ????????????
     * @return List<PostListDTO> ??????DTO??????
     */
    private List<PostListDto> getPostListDTOS(boolean isLogin, List<Post> records) {
        // ???????????????
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                CORE_SIZE + 1,
                2 * CORE_SIZE + 1,
                1L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new NamingThreadFactory(this.getClass().getName() + "-thread"));
        try {
            List<PostListDto> postDTOList = new ArrayList<>(records.size());
            //????????????????????????????????????DTO
            CountDownLatch countDownLatch = new CountDownLatch(records.size());
            for (Post record : records) {
                Long loginId = authService.getLoginUid();
                poolExecutor.submit(() -> {
                    try {
                        getPostInfo(isLogin, postDTOList, record, loginId);
                    } catch (Exception e) {
                        log.error("?????????????????????????????????{}", e.getMessage());
                    } finally {
                        // ???????????????????????????????????????1
                        countDownLatch.countDown();
                    }
                });
            }
            countDownLatch.await();
            return postDTOList;
        } catch (RuntimeException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            poolExecutor.shutdown();
        }
    }

    /**
     * ???????????????????????????
     *
     * @param isLogin     ??????????????????
     * @param postDTOList ??????DTO??????
     * @param record      ????????????
     */
    private void getPostInfo(boolean isLogin, List<PostListDto> postDTOList, Post record, Long loginId) throws InterruptedException {
        // ???????????????
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(
                CORE_SIZE + 1,
                2 * CORE_SIZE + 1,
                1L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new NamingThreadFactory(this.getClass().getName() + "-thread"));


        PostListDto postListDTO = new PostListDto();
        BeanUtils.copyProperties(record, postListDTO);

        CompletableFuture<PostListDto> postListDTOCompletableFuture = CompletableFuture.supplyAsync(() -> {
            postListDTO.setAuthorInfo(memberFeignService.getMemberByUid(record.getMemberUid()));
            return postListDTO;
        },poolExecutor);

        CompletableFuture<PostListDto> postListDTOCompletableFuture2 = CompletableFuture.supplyAsync(() -> {
            postListDTO.setThumbCount(iPostManage.getThumbCount(record.getUid()));
            postListDTO.setCommentCount(iPostManage.getCommentCount(record.getUid()));
            if (isLogin) {
                postListDTO.setIsLike(iPostManage.checkIsThumbOrCollect(record.getUid(), loginId, 0));
                postListDTO.setIsCollect(iPostManage.checkIsThumbOrCollect(record.getUid(), loginId, 1));
            } else {
                postListDTO.setIsLike(false);
                postListDTO.setIsCollect(false);
            }
            return postListDTO;
        },poolExecutor);

        postListDTOCompletableFuture.thenCombine(postListDTOCompletableFuture2, (postListDTO1, postListDTO2) -> postListDTO1)
                .thenAccept(postDTOList::add).exceptionally(ex->{
                    log.error("?????????????????????????????????{}", ex.getMessage());
                    poolExecutor.shutdown();
                    return null;
                }).join();

        poolExecutor.shutdown();
    }

    /**
     * ??????/????????????
     *
     * @param postVo ??????vo
     * @return ????????????
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result savePost(PostVo postVo) {
        Category category = categoryDao.selectById(postVo.getCategoryUid());
        if (category == null) {
            return Result.error("???????????????");
        }

        Set<Long> tagUid = new HashSet<>(postVo.getTagUid());
        if (CollUtil.isEmpty(tagUid)) {
            return Result.error("???????????????");
        } else {
            List<Tag> tags = tagDao.selectBatchIds(tagUid);
            tags.forEach(tag -> {
                if (tag == null) {
                    throw new RuntimeException("???????????????");
                }
            });
        }

        Post post = new Post();

        //????????????????????????Html??????
        String htmlContent = postVo.getContentHtml();
        String text = Html2TextUtil.Html2Text(htmlContent);

        if (CharSequenceUtil.isEmpty(postVo.getSummary())) {
            String summary = CharSequenceUtil.sub(text, 0, 150);
            post.setSummary(summary);
        } else {
            post.setSummary(postVo.getSummary());
        }

        post.setIsPublish(Boolean.TRUE.equals(postVo.getIsPublish()) ? PostConstant.PUBLISH : PostConstant.NOT_PUBLISH);
        post.setTitle(postVo.getTitle());
        post.setContent(postVo.getContent());
        post.setCategoryUid(postVo.getCategoryUid());
        post.setMemberUid(authService.getLoginUid());
        post.setStatus(PostConstant.CHECK_STATUS);

        int result = -1;
        // ?????????????????????????????????????????????
        if (Objects.equals(postVo.getType(), PostConstant.POST_SAVE_TYPE_EDIT)) {
            postTagDao.delete(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostUid, postVo.getUid()));
            post.setUid(postVo.getUid());
            result = postDao.updateById(post);
        } else if (Objects.equals(postVo.getType(), PostConstant.POST_SAVE_TYPE_NEW)) {
            // ????????????
            result = postDao.insert(post);
        }

        if (result == 1) {
            //??????????????????????????????????????????????????????
            rabbitmqUtil.reviewPost(post, new ArrayList<>(tagUid), Objects.equals(postVo.getType(), PostConstant.POST_SAVE_TYPE_EDIT));
            log.info("???????????????ID??????{}", post.getUid());
            return Result.ok().put("url", post.getUid());
        } else {
            log.error("??????={}??????????????????,???????????????={}", authService.getLoginUid(), postVo);
            return Result.error("??????????????????");
        }
    }

    /**
     * ????????????????????????uid
     *
     * @param uid ??????uid
     * @return ????????????
     */
    @Override
    public Result getPostByUid(Long uid) {

        boolean isLogin = authService.getIsLogin();

        String key = RedisKeyConstant.POST_CACHE_KEY + uid;
        Post post;

        if (redisUtil.hasKey(key)) {
            return Result.error("???????????????");
        }
        post = postDao.selectById(uid);
        // ??????????????????
        if (post == null) {
            redisUtil.set(key, null, 60L * 60 * 3);
            return Result.error("???????????????");
        }

        if (!isLogin || !Objects.equals(post.getMemberUid(), authService.getLoginUid())) {
            if (Objects.equals(post.getIsPublish(), PostConstant.NOT_PUBLISH)) {
                return Result.error("????????????????????????");
            }
            if (!Objects.equals(post.getStatus(), PostConstant.NORMAL_STATUS)) {
                return Result.error("???????????????????????????????????????");
            }
        }

        List<MemberDto> memberInfoWithPost = memberFeignService
                .batchQueryUsers(CollUtil.toList(post.getMemberUid()));

        PostDto postDTO = new PostDto();
        BeanUtils.copyProperties(post, postDTO);

        // ?????????????????????
        List<PostTag> tags = postTagDao.selectList(new LambdaQueryWrapper<PostTag>().eq(PostTag::getPostUid, uid));

        List<Long> tagUid = tags.stream().map(PostTag::getTagUid).collect(Collectors.toList());

        postDTO.setTagUid(tagUid);
        if (CollUtil.isNotEmpty(memberInfoWithPost)) {
            postDTO.setAuthorInfo(memberInfoWithPost.get(0));
            if (isLogin) {
                postDTO.setIsCollect(iPostManage.checkIsThumbOrCollect(uid, authService.getLoginUid(), 1));
                postDTO.setIsLike(iPostManage.checkIsThumbOrCollect(uid, authService.getLoginUid(), 0));
            } else {
                postDTO.setIsCollect(false);
                postDTO.setIsLike(false);
            }
        }

        return Objects.requireNonNull(Result.ok().put("data", postDTO));
    }

    /**
     * ????????????
     *
     * @param postUid ??????uid
     * @return ????????????
     */
    @Override
    public Result deletePost(Long postUid) {
        if (postUid == null) {
            return Result.error("??????uid????????????");
        }

        Post post = postDao.selectById(postUid);

        if (!Objects.equals(post.getMemberUid(), authService.getLoginUid())) {
            return Result.error();
        }

        postDao.deleteById(postUid);
        //??????es????????????????????????
        searchFeignService.deleteSearchPost(post.getUid());
        return Result.ok();
    }

    /**
     * ????????????uid???????????????
     *
     * @param queryParam ????????????
     * @return ????????????
     */
    @Override
    public Result listByMemberUid(QueryParam queryParam) {

        IPage<Post> page = null;
        if (!authService.getIsLogin() || !Objects.equals(queryParam.getUid(), authService.getLoginUid())) {
            page = this.page(new Query<Post>()
                    .getPage(queryParam), new LambdaQueryWrapper<Post>()
                    .eq(Post::getMemberUid, queryParam.getUid())
                    .eq(Post::getStatus, PostConstant.NORMAL_STATUS)
                    .eq(Post::getIsPublish, PostConstant.PUBLISH));
        } else if (Objects.equals(queryParam.getUid(), authService.getLoginUid())) {
            page = this.page(new Query<Post>()
                    .getPage(queryParam), new LambdaQueryWrapper<Post>()
                    .eq(Post::getMemberUid, queryParam.getUid())
                    .eq(Post::getStatus, PostConstant.NORMAL_STATUS));
        }

        assert page != null;
        List<Post> postList = page.getRecords();
        if (CollUtil.isEmpty(postList)) {
            return Result.ok().put("data", new PageUtils(page));
        }
        List<PostListDto> postListDtos = new ArrayList<>(10);
        MemberDto memberDTO = memberFeignService.getMemberByUid(queryParam.getUid());

        postList.forEach(postEntity -> {
            PostListDto postListDTO = new PostListDto();
            BeanUtils.copyProperties(postEntity, postListDTO);
            postListDTO.setAuthorInfo(memberDTO);
            postListDTO.setThumbCount(iPostManage.getThumbCount(postEntity.getUid()));
            if (authService.getIsLogin()) {
                postListDTO.setIsLike(iPostManage.checkIsThumbOrCollect(postEntity.getUid(), authService.getLoginUid(), 0));
                postListDTO.setIsCollect(iPostManage.checkIsThumbOrCollect(postEntity.getUid(), authService.getLoginUid(), 1));
            } else {
                postListDTO.setIsLike(false);
                postListDTO.setIsCollect(false);
            }
            postListDtos.add(postListDTO);
        });
        IPage<PostListDto> pagePostListDTO = new Page<>();
        BeanUtils.copyProperties(page, pagePostListDTO);

        pagePostListDTO.setRecords(postListDtos);

        PageUtils pageResult = new PageUtils(pagePostListDTO);
        return Result.ok().put("data", pageResult);
    }
}