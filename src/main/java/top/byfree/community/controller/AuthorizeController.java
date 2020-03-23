package top.byfree.community.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import top.byfree.community.dto.AccessTokenDTO;
import top.byfree.community.dto.GithubUser;
import top.byfree.community.model.User;
import top.byfree.community.provider.GithubProvider;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class AuthorizeController {

    @Autowired
    private GithubProvider githubProvider;
    @Value("${github.client.id}")
    private String clientId;
    @Value("${github.client.secret}")
    private String clientSecret;
    @Value("${github.redirect.uri}")
    private String redirectUri;
    @Autowired
    private top.byfree.community.mapper.userMapper userMapper;

    @GetMapping("/callback")
    public String callback(@RequestParam(name = "code") String code,
                           @RequestParam(name = "state") String state,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        // 创建AccessTokenDTO类的实例
        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        // 设置各种属性
        accessTokenDTO.setCode(code);
        accessTokenDTO.setClient_id(clientId);
        accessTokenDTO.setClient_secret(clientSecret);
        accessTokenDTO.setRedirect_uri(redirectUri);
        accessTokenDTO.setState(state);
        // getAccessToken：携带AccessTokenDTO中的属性向github发送一个post请求返回一个字符串包含accessToken
        String accessToken = githubProvider.getAccessToken(accessTokenDTO);
        // getUser：携带accessToken想github发送一个get请求得到一个JSON封装到GithubUser实体类中去
        GithubUser githubUser = githubProvider.getUser(accessToken);
        // 判断登陆是否成功
        if (githubUser != null) {
            // 封装一个User对象
            User user = new User();
            // 使用这个token代替session
            String token = UUID.randomUUID().toString();
            user.setToken(token);
            user.setName(githubUser.getName());
            user.setAccountId(String.valueOf(githubUser.getId()));
            user.setGmtCreate(System.currentTimeMillis());
            user.setGmtModified(user.getGmtCreate());
            // userMapper：向数据库中写如user
            userMapper.insert(user);
            // 写cookie，和session
            response.addCookie(new Cookie("token", token));

            // 重定向
            return "redirect:/";
        } else {
            // 登陆失败，重新登陆
            return "redirect:/";

        }
    }
}
