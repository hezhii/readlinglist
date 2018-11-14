# Spring Boot 实战

《Spring Boot 实战》中的示例程序。

跟着书中的示例代码进行，时不时会遇到一些不能正常运行的情况，从出版社[网站](https://www.manning.com/books/spring-boot-in-action)上下载的示例代码感觉也加了很多多余的东西。
我感觉可能是第四版太老了，很多地方都发生了变化。所以，我结合了书中的示例然后加入了自己的理解。

## 遇到的一些问题

编写示例程序的过程中，遇到了若干的问题，记录下来用于以后回顾。

### 第三章：自定义配置

**1.默认的 Spring Security 提供了一个登录页面，而不是书中所说的是 HTTP 基础认证。应该是和书中 Spring Security 版本不同。**

**2.覆盖默认配置后，登录页面无法正常访问了。**

配置如下：
```java
@Override
protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
            .antMatchers("/").access("hasRole('READER')")
            .and()
            .formLogin()
            .loginPage("/login")
            .failureUrl("/login?error=true");
}
```

这里需要提供 `/login` 对应的页面（thymeleaf 模板），然后在控制器中返回。

```java
@RequestMapping("/login")
public String loginPage() {
    return "login";
}
```

**3.提供 `data.sql` 文件，Spring Boot 会帮我们使用里面的 DCL 初始化数据库。**

关于 Spring Boot 中如何初始化数据库，我记录在了[博客](https://blog.whezh.com/spring-boot-database-initialization)中。

**4.UserDetailsService 中需要使用加密后的密码进行匹配，或者会报错：`there is no passwordencoder mapped for the id null`。**

```java
 auth.userDetailsService(new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username)
                    throws UsernameNotFoundException {
                Reader reader = readerRepository.findByUsername(username);
                if (reader == null) {
                    throw new UsernameNotFoundException("User '" + username + "' not found.");
                }
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                /*
                 * passwordEncoder(new BCryptPasswordEncoder())：登录时会把页面传递过来的密码加密后再与数据库中的匹配。
                 * 必须要设置加密否则会报错：there is no passwordencoder mapped for the id null
                 *
                 * 而 data.sql 中写入的是明文密码，所有这里在返回 UserDetails 时，手动加密一下密码。
                 * 正常情况下，写入到数据中的密码就是加密过的，这里需要处理，直接返回查到的 UserDetails 即可。
                 */
                reader.setPassword(encoder.encode(reader.getPassword()));
                return reader;
            }
        }).passwordEncoder(new BCryptPasswordEncoder());
```

**5.设置用户的角色时，应该已 `ROLE_` 开头，否则角色无法匹配。**

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    // Error: return Arrays.asList(new SimpleGrantedAuthority("READER"));
    return Arrays.asList(new SimpleGrantedAuthority("ROLE_READER"));
}
```

**6.无法提交书单信息，返回 403。**

> 参考《Spring 实战》（第四版）中 9.9.9 节。

Spring Security 3.2 开始，默认启用 CSRF 防护。这就意味着，在创建书单时，需要提交 csrf token，并且这个 token 要与服务端计算并存储的 token 一致。

如果使用 Thymeleaf 作为页面模板的话，只要 `<form>` 标签的 action 属性添加了额 Thymeleaf 命名空间前缀，那么就会自动生成一个 “_csrf” 隐藏域。
```html
<form method="POST" th:action="@{/}">
  ...
</form>
```

如果使用 JSP 作为页面模板的话，则需要手动添加隐藏表单域：
```jsp
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
```