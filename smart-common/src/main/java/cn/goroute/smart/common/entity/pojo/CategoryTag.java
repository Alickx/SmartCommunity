package cn.goroute.smart.common.entity.pojo;

import cn.goroute.smart.common.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Transient;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 分类标签关联表
 * @author Alickx
 * @TableName t_section_tag
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_category_tag")
public class CategoryTag extends BaseEntity implements Serializable {

    /**
     * 分类id
     */
    @NotNull(message = "分类id不能为空")
    private Long categoryUid;

    /**
     * 标签id
     */
    @NotNull(message = "标签id不能为空")
    private Long tagUid;

    @Transient
    private static final long serialVersionUID = 1L;
}