package com.newyear.mainproject.member.service;

import com.newyear.mainproject.board.repository.BoardRepository;
import com.newyear.mainproject.comment.repository.CommentRepository;
import com.newyear.mainproject.exception.BusinessLogicException;
import com.newyear.mainproject.exception.ExceptionCode;
import com.newyear.mainproject.member.entity.Member;
import com.newyear.mainproject.member.repository.MemberRepository;
import com.newyear.mainproject.security.utils.CustomAuthorityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthorityUtils authorityUtils;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;

    public Member createMember(Member member){
        verifyExistsEmail(member.getEmail());

        //패스워드 암호화
        String encryptedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encryptedPassword);

        List<String> roles = authorityUtils.createRoles(member.getEmail());
        member.setRoles(roles);

        Member savedMember = memberRepository.save(member);

        return savedMember;
    }

    public Member updateMember(Member member){
        Member findMember = findVerifiedMember(member.getMemberId());

        Optional.ofNullable(member.getDisplayName())
                .ifPresent(name -> findMember.setDisplayName(name));

        Optional.ofNullable(member.getMemberStatus())
                .ifPresent(memberStatus -> findMember.setMemberStatus(memberStatus));

        Optional.ofNullable(member.getPassword())
                .ifPresent(password -> findMember.setPassword(
                        passwordEncoder.encode(password)
                ));

        return memberRepository.save(findMember);
    }

    public Member updatePasswordMember(Member member){
        Member findMember = findVerifiedMember(member.getMemberId());

        Optional.ofNullable(member.getPassword())
                .ifPresent(password -> findMember.setPassword(
                        passwordEncoder.encode(password)
                ));

        return memberRepository.save(findMember);
    }

    public Member updateDisplayName(Member member){
        Member findMember = findVerifiedMember(member.getMemberId());

        Optional.ofNullable(member.getDisplayName())
                .ifPresent(name -> findMember.setDisplayName(name));

        return memberRepository.save(findMember);
    }

    public Member findMember(long memberId){
        return findVerifiedMember(memberId);
    }

    public Page<Member> findMembers(int page, int size){
        return memberRepository.findAll(PageRequest.of(page, size,
                Sort.by("memberId").descending()));
    }

    public void deleteMember(long memberId) {
        Member member = findVerifiedMember(memberId);
        member.setMemberStatus(Member.MemberStatus.MEMBER_QUIT);
        memberRepository.save(member);

        //회원 탈퇴시 board, comment 삭제
        boardRepository.deleteAll(member.getBoards());
        commentRepository.deleteAll(member.getComments());
    }

    private void verifyExistsEmail(String email){
        Optional<Member> optionalMember = memberRepository.findByEmail(email);
        if(optionalMember.isPresent()){
            throw new BusinessLogicException(ExceptionCode.MEMBER_EXISTS);
        }
    }

    public Member findVerifiedMember(long memberId){
        Optional<Member> optionalMember = memberRepository.findById(memberId);
        Member member = optionalMember.orElseThrow(() ->
                new BusinessLogicException(ExceptionCode.MEMBER_NOT_FOUND));
        if(member.getMemberStatus() == Member.MemberStatus.MEMBER_QUIT
                || member.getMemberStatus() == Member.MemberStatus.MEMBER_SLEEP){
            throw new BusinessLogicException(ExceptionCode.INVALID_MEMBER_STATUS);
        }

        return member;
    }

    private String findLoginMemberEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    // 로그인 유저 얻기
    public Member getLoginMember() {
        Optional<Member> optionalMember = memberRepository.findByEmail(findLoginMemberEmail());
        Member member = optionalMember.orElseThrow(() -> new BusinessLogicException(ExceptionCode.MEMBER_NOT_FOUND));
        return member;
    }
}
