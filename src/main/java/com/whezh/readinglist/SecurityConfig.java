package com.whezh.readinglist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private ReaderRepository readerRepository;

    @Autowired
    public SecurityConfig(ReaderRepository readerRepository) {
        this.readerRepository = readerRepository;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/").access("hasRole('READER')")
                .and()
                .formLogin()
                .loginPage("/login")
                .failureUrl("/login?error=true");
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
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
    }
}
