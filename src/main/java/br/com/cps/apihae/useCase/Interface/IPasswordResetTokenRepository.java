package br.com.cps.apihae.useCase.Interface;

import java.util.Optional;

import br.com.cps.apihae.domain.entity.Employee;
import br.com.cps.apihae.domain.entity.PasswordResetToken;

public interface IPasswordResetTokenRepository {
    Optional<PasswordResetToken> findByToken(String token);

    void deleteByEmployee(Employee employee);

    PasswordResetToken save(PasswordResetToken token);

    void delete(PasswordResetToken token);
}
