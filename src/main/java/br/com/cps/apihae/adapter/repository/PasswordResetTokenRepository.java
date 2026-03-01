package br.com.cps.apihae.adapter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cps.apihae.domain.entity.PasswordResetToken;
import br.com.cps.apihae.useCase.Interface.IPasswordResetTokenRepository;

@Repository
public interface PasswordResetTokenRepository extends IPasswordResetTokenRepository, JpaRepository<PasswordResetToken, Long> {
}