package br.com.cps.apihae.adapter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cps.apihae.domain.entity.EmailVerification;
import br.com.cps.apihae.useCase.Interface.IEmailVerificationRepository;


@Repository
public interface EmailVerificationRepository extends IEmailVerificationRepository, JpaRepository<EmailVerification, String>  {
}