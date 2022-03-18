package cn.goroute.smart.post.controller;

import cn.goroute.smart.common.utils.Result;
import cn.goroute.smart.post.service.SectionTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("post/sectiontag")
public class SectionTagController {

    @Autowired
    SectionTagService sectionTagService;


    @GetMapping("/list")
    public Result getTagBySection(@RequestParam Long sectionUid){

        return sectionTagService.getTagBySection(sectionUid);

    }

}