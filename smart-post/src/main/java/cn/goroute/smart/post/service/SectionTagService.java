package cn.goroute.smart.post.service;

import cn.goroute.smart.common.entity.pojo.SectionTag;
import cn.goroute.smart.common.utils.Result;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Alickx
* @description 针对表【t_section_tag(分类标签关联表)】的数据库操作Service
* @createDate 2022-03-04 16:06:54
*/
public interface SectionTagService extends IService<SectionTag> {

    Result getTagBySection(Long sectionUid);
}
