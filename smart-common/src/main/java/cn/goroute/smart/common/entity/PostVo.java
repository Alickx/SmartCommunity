package cn.goroute.smart.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostVo {

    /**
     *
     */
    private String sectionUid;

    private List<String> tagUid;
    /**
     *
     */
    @NotBlank
    private String title;
    /**
     *
     */
    @NotBlank
    private String content;
    /**
     *
     */
    private String headImg;
    /**
     * 0 = 不公布  1 = 公布
     */
    private Boolean isPublish;
    /**
     *
     */
    private String summary;
}