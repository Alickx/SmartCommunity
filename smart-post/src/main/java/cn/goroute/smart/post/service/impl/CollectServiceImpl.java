package cn.goroute.smart.post.service.impl;

import cn.goroute.smart.common.dao.CollectDao;
import cn.goroute.smart.common.entity.pojo.Collect;
import cn.goroute.smart.post.service.CollectService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service("collectService")
public class CollectServiceImpl extends ServiceImpl<CollectDao, Collect> implements CollectService {

}