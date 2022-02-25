package cn.goroute.smart.post.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.goroute.smart.common.utils.PageUtils;
import cn.goroute.smart.common.utils.Query;

import cn.goroute.smart.post.dao.PostTagDao;
import cn.goroute.smart.post.entity.PostTagEntity;
import cn.goroute.smart.post.service.PostTagService;


@Service("postTagService")
public class PostTagServiceImpl extends ServiceImpl<PostTagDao, PostTagEntity> implements PostTagService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PostTagEntity> page = this.page(
                new Query<PostTagEntity>().getPage(params),
                new QueryWrapper<PostTagEntity>()
        );

        return new PageUtils(page);
    }

}