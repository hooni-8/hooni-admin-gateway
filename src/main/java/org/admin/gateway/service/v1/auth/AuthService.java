package org.admin.gateway.service.v1.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.admin.gateway.common.model.LoginStatus;
import org.admin.gateway.entity.user.UserDetail;
import org.admin.gateway.filters.jwt.JwtTokenProvider;
import org.admin.gateway.model.v1.auth.request.LoginRequest;
import org.admin.gateway.model.v1.auth.request.RefreshAccessTokenRequest;
import org.admin.gateway.model.v1.auth.request.RegisterRequest;
import org.admin.gateway.model.v1.auth.response.AuthResponse;
import org.admin.gateway.repository.auth.AuthRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final AuthRepository authRepository;

    private final JwtTokenProvider jwtTokenProvider;

    private final RefreshTokenService refreshTokenService;

    // 회원가입
    public Mono<Void> register(RegisterRequest registerRequest) {

        String userCode = UUID.randomUUID().toString();

        UserDetail userDetail = UserDetail.builder()
                .userCode(userCode)
                .userId(registerRequest.getUserId())
                .userPw(encodePassword(registerRequest.getUserPassword()))
                .email(registerRequest.getUserEmail())
                .userName(registerRequest.getUserName())
                .build();

        return authRepository.insert(userDetail).then();
    }

    // 비밀번호 암호화
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    // 로그인
    public Mono<AuthResponse> login(LoginRequest loginRequest) {
        return userDetails(loginRequest.getUserId())
                .doOnNext(user -> log.info("조회된 유저: {}", user))
                .doOnError(err -> log.error("userDetails 에러 발생", err))
                .flatMap(user -> handleLogin(user, loginRequest.getPassword()))
                .switchIfEmpty(Mono.just(AuthResponse.getLoginFail()));
    }

    // 로그인 토큰 발급
    private Mono<AuthResponse> handleLogin(UserDetail user, String rawPassword) {
        if (matches(rawPassword, user.getUserPw())) {
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            log.info("AccessToken: {}", accessToken);
            log.info("RefreshToken: {}", refreshToken);

            // 로그인 성공 시 refreshToken 저장
//            refreshTokenService.saveRefreshToken(user.getUserCode(), refreshToken);

            return Mono.just(AuthResponse.getSuccess(accessToken, refreshToken));
        }
        return Mono.just(AuthResponse.getLoginFail());
    }

    // refresh Token 발급
    public Mono<AuthResponse> refreshToken(RefreshAccessTokenRequest tokenRequest, String userCode) {
        return userDetailsFindByUserCode(userCode)
                .map(user -> {
                    String newAccessToken = jwtTokenProvider.generateAccessToken(user);
                    return AuthResponse.getSuccess(newAccessToken, tokenRequest.getRefreshToken());
                });
    }

    // userId를 이용해서 DB 조회
    public Mono<UserDetail> userDetails(String userId) {
        return authRepository.findByUserId(userId);
    }

    public Mono<UserDetail> userDetailsFindByUserCode(String userCode) {
        return authRepository.findByUserCode(userCode);
    }

    // 비밀번호 matches
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // userId 중복 확인
    public Mono<Boolean> existsUserId(String userId) {
        return authRepository.existsByUserId(userId);
    }

    public LoginStatus getSession(String token) {
        return jwtTokenProvider.getClaims(token);
    }

    public String getClaimsUserCode(String token) {
        return jwtTokenProvider.getClaimsUserCode(token);
    }

}
