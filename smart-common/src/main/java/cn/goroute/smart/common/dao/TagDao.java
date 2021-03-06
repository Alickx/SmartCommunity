package cn.goroute.smart.common.dao;

import cn.goroute.smart.common.entity.pojo.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签表
 * 
 * @author Alickx
 * @email llwstu@gmail.com
 * @date 2022-02-25 09:44:39
 */
@Mapper
public interface TagDao extends BaseMapper<Tag> {
	
}
