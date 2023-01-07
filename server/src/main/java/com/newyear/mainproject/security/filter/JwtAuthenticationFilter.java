package com.newyear.mainproject.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newyear.mainproject.exception.BusinessLogicException;
import com.newyear.mainproject.exception.ExceptionCode;
import com.newyear.mainproject.member.entity.Member;
import com.newyear.mainproject.member.repository.MemberRepository;
import com.newyear.mainproject.security.dto.LoginDto;
import com.newyear.mainproject.security.jwt.JwtTokenizer;
import com.newyear.mainproject.security.logout.RedisUtil;
import com.newyear.mainproject.security.logout.RefreshToken;
import com.newyear.mainproject.security.logout.RefreshTokenRepository;
import lombok.SneakyThrows;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenizer jwtTokenizer;
    private final MemberRepository memberRepository;
    private final RedisUtil redisUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager,
                                   JwtTokenizer jwtTokenizer, MemberRepository memberRepository,
                                   RedisUtil redisUtil, RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenizer = jwtTokenizer;
        this.memberRepository = memberRepository;
        this.redisUtil = redisUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    //메서드 내부에서 인증을 시도하는 로직
    @SneakyThrows
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {

        //클라이언트에서 전송한 Username과 Password를 DTO 클래스로 역직렬화(Deserialization)하기 위해 ObjectMapper 인스턴스를 생성
        ObjectMapper objectMapper = new ObjectMapper();
        //objectMapper.readValue(request.getInputStream(), LoginDto.class)를 통해 ServletInputStream 을 LoginDto 클래스의 객체로 역직렬화(Deserialization)
        LoginDto loginDto = objectMapper.readValue(request.getInputStream(), LoginDto.class);

        //회원 상태가 ACTIVE 가 아니면 예외처리
        Member member = memberRepository.findByEmail(loginDto.getEmail()).get();
        if(member.getMemberStatus() != Member.MemberStatus.MEMBER_ACTIVE){
            logger.info("정지 or 휴면 상태인 회원은 로그인 불가");
            throw new BusinessLogicException(ExceptionCode.INVALID_MEMBER_STATUS);
        }

        //Username과 Password 정보를 포함한 UsernamePasswordAuthenticationToken을 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword());

        //UsernamePasswordAuthenticationToken을 AuthenticationManager에게 전달하면서 인증 처리
        return authenticationManager.authenticate(authenticationToken);
    }

    //클라이언트의 인증 정보를 이용해 인증에 성공할 경우 호출
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws ServletException, IOException {
        //authResult.getPrincipal()로 Member 엔티티 클래스의 객체를 얻음
        Member member = (Member) authResult.getPrincipal();

        //Access Token을 생성
        String accessToken = delegateAccessToken(member);
        //Refresh Token을 생성
        String refreshToken = delegateRefreshToken(member);

        //response header(Authorization)에 Access Token을 추가.
        // Access Token은 클라이언트 측에서 백엔드 애플리케이션 측에 요청을 보낼때 마다 request header에 추가해서 클라이언트 측의 자격 증명 용도로 사용
        response.setHeader("Authorization", "Bearer " + accessToken);

        //response header(Refresh)에 Refresh Token을 추가
        //Refresh Token은 Access Token이 만료될 경우, Access Token을 새로 발급받기 위한 용도이며
        //Refresh Token을 Access Token과 함께 클라이언트에게 제공할 지 여부는 애플리케이션의 요구 사항에 따라 달라질 수 있음
        response.setHeader("Refresh", refreshToken);



        this.getSuccessHandler().onAuthenticationSuccess(request, response, authResult);
    }

    //Access Token을 생성
    private String delegateAccessToken(Member member) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", member.getEmail());
        claims.put("roles", member.getRoles());

        System.out.println("엑세스토큰 생성됨★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

        String subject = member.getEmail();
        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getAccessTokenExpirationMinutes());

        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());

        String accessToken = jwtTokenizer.generateAccessToken(claims, subject, expiration, base64EncodedSecretKey);

        return accessToken;
    }

    //Refresh Token을 생성
    private String delegateRefreshToken(Member member) {
        String subject = member.getEmail();
        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getRefreshTokenExpirationMinutes());
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());

        System.out.println("리프레쉬토큰 생성됨★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");

        String refreshToken = jwtTokenizer.generateRefreshToken(subject, expiration, base64EncodedSecretKey);

        RefreshToken token = new RefreshToken(refreshToken);
        token.setEndedAt(LocalDateTime.now());
        token.setEndedAt(token.getEndedAt().plusMinutes(jwtTokenizer.getAccessTokenExpirationMinutes()));

        refreshTokenRepository.save(token);

        return refreshToken;
    }
}