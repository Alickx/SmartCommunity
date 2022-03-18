package cn.goroute.smart.search.feign;

import cn.goroute.smart.common.entity.dto.MemberDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("smart-member")
public interface MemberFeignService {

    @GetMapping("/member/member/getMemberByUid")
    MemberDTO getMemberByUid(@RequestParam String uid);

}
