package cn.goroute.smart.post.manage.impl;

import cn.goroute.smart.common.constant.PostConstant;
import cn.goroute.smart.common.constant.RedisKeyConstant;
import cn.goroute.smart.common.dao.CollectDao;
import cn.goroute.smart.common.dao.PostDao;
import cn.goroute.smart.common.dao.ThumbDao;
import cn.goroute.smart.common.entity.pojo.Collect;
import cn.goroute.smart.common.entity.pojo.Post;
import cn.goroute.smart.common.entity.pojo.Thumb;
import cn.goroute.smart.common.feign.MemberFeignService;
import cn.goroute.smart.common.utils.RedisUtil;
import cn.goroute.smart.post.manage.IPostManage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: Alickx
 * @Date: 2022/06/08/8:06
 * @Description:
 */
@Service
public class PostManageImpl implements IPostManage {

    @Autowired
    PostDao postDao;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    CollectDao collectDao;

    @Autowired
    ThumbDao thumbDao;

    @Autowired
    MemberFeignService memberFeignService;


    /**
     * 获取是否点赞或收藏
     *
     * @param uid      目标uid
     * @param loginUid 用户uid
     * @param type     类型
     * @return 是否点赞或是否收藏
     */
    @Override
    public boolean checkIsThumbOrCollect(Long uid, Long loginUid, int type) {
        boolean result = false;
        /*
          判断是否点赞或是否收藏
         */
        if (type == 0) {
            String thumbRedisKey = RedisKeyConstant.getThumbKey(loginUid, uid);
            if (redisUtil.hHasKey(RedisKeyConstant.POST_THUMB_KEY, thumbRedisKey)) {
                result = true;
            } else {
                //如果缓存不存在则去数据库中获取
                Thumb thumbResult = thumbDao.selectOne(new LambdaQueryWrapper<Thumb>()
                        .eq(Thumb::getMemberUid, loginUid)
                        .eq(Thumb::getType, PostConstant.THUMB_POST_TYPE)
                        .eq(Thumb::getPostUid, uid));
                if (thumbResult != null) {
                    result = true;
                }
            }
        } else if (type == 1) {
            String collectRedisKey = RedisKeyConstant.getThumbKey(loginUid, uid);
            if (redisUtil.hHasKey(RedisKeyConstant.POST_COLLECT_KEY, collectRedisKey)) {
                result = true;
            } else {
                Collect collectResult = collectDao.selectOne(new LambdaQueryWrapper<Collect>()
                        .eq(Collect::getMemberUid, loginUid)
                        .eq(Collect::getPostUid, uid));
                if (collectResult != null) {
                    result = true;
                }
            }
        } else {
            return false;
        }
        return result;
    }

    /**
     * 获取文章的总点赞
     *
     * @param post 文章实体
     * @return 点赞数
     */
    @Override
    public int getThumbCount(Post post) {

        //先从缓存中获取再去数据库中获取
        String key = RedisKeyConstant.POST_COUNT_KEY + post.getUid();
        if (redisUtil.hHasKey(key, RedisKeyConstant.POST_THUMB_COUNT_KEY)) {
            return (int) redisUtil.hget(key, RedisKeyConstant.POST_THUMB_COUNT_KEY);
        }

        //缓存获取不到就数据库获取
        int thumbCount;
        synchronized (this) {
            if (redisUtil.hHasKey(key, RedisKeyConstant.POST_THUMB_COUNT_KEY)) {
                return (int) redisUtil.hget(key, RedisKeyConstant.POST_THUMB_COUNT_KEY);
            }
            Post postEntity = postDao.selectOne(new QueryWrapper<Post>().eq("uid", post.getUid()));
            thumbCount = postEntity.getThumbCount();
            redisUtil.hset(key, RedisKeyConstant.POST_THUMB_COUNT_KEY, thumbCount);
        }
        return thumbCount;
    }

    /**
     * 获取文章的评论总数
     *
     * @param post 文章实体类
     * @return 文章评论总数
     */
    @Override
    public int getCommentCount(Post post) {

        Long postUid = post.getUid();

        String key = RedisKeyConstant.POST_COUNT_KEY + postUid;
        if (redisUtil.hHasKey(key, RedisKeyConstant.POST_COMMENT_COUNT_KEY)) {
            return (int) redisUtil.hget(key, RedisKeyConstant.POST_COMMENT_COUNT_KEY);
        }
        int commentCount;
        synchronized (this) {
            if (redisUtil.hHasKey(key, RedisKeyConstant.POST_COMMENT_COUNT_KEY)) {
                return (int) redisUtil.hget(key, RedisKeyConstant.POST_COMMENT_COUNT_KEY);
            }
            Post postEntity = postDao.selectOne(new QueryWrapper<Post>().eq("uid", post.getUid()));
            commentCount = postEntity.getCommentCount();
            redisUtil.hset(key, RedisKeyConstant.POST_COMMENT_COUNT_KEY, commentCount);
        }
        return commentCount;
    }
}
