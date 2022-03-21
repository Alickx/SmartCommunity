package cn.goroute.smart.gateway.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.goroute.smart.common.dao.MemberDao;
import cn.goroute.smart.common.entity.dto.MemberDTO;
import cn.goroute.smart.common.entity.pojo.MemberEntity;
import cn.goroute.smart.common.entity.vo.MemberLoginVO;
import cn.goroute.smart.common.utils.Result;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/login")
public class LoginApi {

    @Autowired
    MemberDao memberDao;

    @PostMapping
    public Result login(@RequestBody MemberLoginVO memberLoginVo) {

        String username = memberLoginVo.getUsername();

        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("email", username));


        if (memberEntity == null) {
            return Result.error("用户名不存在");
        }

        if (!DigestUtil.bcryptCheck(memberLoginVo.getPassWord(), memberEntity.getPassWord())) {
            return Result.error("用户名或密码错误");
        }


        MemberDTO memberDTO = new MemberDTO();
        BeanUtils.copyProperties(memberEntity,memberDTO);

        StpUtil.login(memberEntity.getUid());

        return Result.ok()
                .put("access_token", StpUtil.getTokenValue())
                .put("user_info",memberDTO)
                .put("permission_list",StpUtil.getPermissionList())
                .put("role_list", StpUtil.getRoleList());
    }

}
