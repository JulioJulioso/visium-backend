package com.visium.backend.service;

import com.visium.backend.entity.PasswordRecoveryCode;
import com.visium.backend.entity.Usuario;
import com.visium.backend.repository.PasswordRecoveryCodeRepository;
import com.visium.backend.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Recupera credenciales sin revelar si un correo existe ni emitir una sesion. */
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
  static final int MAX_ATTEMPTS = 5;
  private static final Duration CODE_TTL = Duration.ofMinutes(15);
  private final UsuarioRepository usuarioRepository;
  private final PasswordRecoveryCodeRepository codeRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Clock clock = Clock.systemUTC();

  @Transactional
  public void request(String rawEmail) {
    String email = normalizeEmail(rawEmail);
    if (email == null) return;

    Usuario user = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
    if (user == null || !Boolean.TRUE.equals(user.getActivo())) return;

    Instant now = clock.instant();
    List<PasswordRecoveryCode> previous = codeRepository.findByEmailIgnoreCaseAndUsedAtIsNull(email);
    previous.forEach(code -> code.setUsedAt(now));
    if (!previous.isEmpty()) codeRepository.saveAll(previous);

    String numericCode = String.format("%06d", secureRandom.nextInt(1_000_000));
    PasswordRecoveryCode recovery = new PasswordRecoveryCode();
    recovery.setEmail(email);
    recovery.setCodeHash(passwordEncoder.encode(numericCode));
    recovery.setExpiresAt(now.plus(CODE_TTL));
    recovery.setAttempts(0);
    recovery.setCreatedAt(now);
    codeRepository.save(recovery);
    emailService.enviarCodigoRecuperacion(email, numericCode);
  }

  @Transactional
  public void confirm(String rawEmail, String code, String newPassword) {
    String email = normalizeEmail(rawEmail);
    if (email == null || !isValidCode(code) || !isValidPassword(newPassword)) return;
    PasswordRecoveryCode recovery = codeRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(email).orElse(null);
    Instant now = clock.instant();
    if (recovery == null || recovery.getUsedAt() != null || !recovery.getExpiresAt().isAfter(now)
        || recovery.getAttempts() >= MAX_ATTEMPTS) return;

    recovery.setAttempts(recovery.getAttempts() + 1);
    if (!passwordEncoder.matches(code, recovery.getCodeHash())) {
      if (recovery.getAttempts() >= MAX_ATTEMPTS) recovery.setUsedAt(now);
      codeRepository.save(recovery);
      return;
    }

    Usuario user = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
    if (user == null || !Boolean.TRUE.equals(user.getActivo())) return;
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    recovery.setUsedAt(now);
    usuarioRepository.save(user);
    codeRepository.save(recovery);
  }

  private String normalizeEmail(String email) {
    if (email == null) return null;
    String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);
    return normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ? normalized : null;
  }

  private boolean isValidCode(String code) { return code != null && code.matches("\\d{6}"); }
  private boolean isValidPassword(String password) { return password != null && password.length() >= 8 && password.length() <= 128; }
}
