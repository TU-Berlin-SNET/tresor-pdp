package org.snet.tresor.pdp.contexthandler;

import javax.inject.Inject;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;

@Configuration
@ComponentScan
@EnableWebSecurity
@EnableWebMvcSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

	@Inject
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication()
			.withUser("broker").password("broker").roles("broker").and()
			.withUser("admin").password("admin").roles("c6b286d2-95e6-460f-a9d1-6b82d3683cfa").and()
			.withUser("herz").password("herz").roles("c6b286d2-95e6-460f-a9d1-6b82d3683cfa").and()
			.withUser("mms").password("mms").roles("92707293-e2e9-4d09-bad1-5c37bdfd0b09");
	}

	protected void configure(HttpSecurity http) throws Exception {
		http.antMatcher("/policy/**").httpBasic().and().csrf().disable();
	}

}
