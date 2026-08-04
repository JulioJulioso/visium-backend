package com.visium.backend.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visium.backend.entity.PasswordRecoveryCode;
import com.visium.backend.entity.Usuario;
import com.visium.backend.repository.PasswordRecoveryCodeRepository;
import com.visium.backend.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {
  @Mock private UsuarioRepository usuarioRepository;
  @Mock private PasswordRecoveryCodeRepository codeRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailService emailService;
  private PasswordRecoveryService service;

  @BeforeEach
  void setUp() {
    service = new PasswordRecoveryService(usuarioRepository, codeRepository, passwordEncoder, emailService);
  }

  @Test
  void requestStoresOnlyHashedCodeAndSendsNumericCode() {
    Usuario user = activeUser();
    when(usuarioRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
    when(codeRepository.findByEmailIgnoreCaseAndUsedAtIsNull("user@example.com")).thenReturn(List.of());
    when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash");

    service.request("USER@example.com");

    ArgumentCaptor<PasswordRecoveryCode> code = ArgumentCaptor.forClass(PasswordRecoveryCode.class);
    verify(codeRepository).save(code.capture());
    assertNotNull(code.getValue().getCodeHash());
    verify(emailService).enviarCodigoRecuperacion(org.mockito.ArgumentMatchers.eq("user@example.com"),
        org.mockito.ArgumentMatchers.matches("\\d{6}"));
  }

  @Test
  void validCodeUpdatesPasswordAndMarksCodeUsedWithoutAuthenticating() {
    Usuario user = activeUser();
    PasswordRecoveryCode recovery = code(Instant.now().plusSeconds(60), null);
    when(codeRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc("user@example.com"))
        .thenReturn(Optional.of(recovery));
    when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
    when(usuarioRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

    service.confirm("user@example.com", "123456", "new-password");

    verify(usuarioRepository).save(user);
    verify(codeRepository).save(recovery);
    org.junit.jupiter.api.Assertions.assertEquals("new-hash", user.getPasswordHash());
    assertNotNull(recovery.getUsedAt());
  }

  @Test
  void expiredCodeDoesNotChangePassword() {
    PasswordRecoveryCode recovery = code(Instant.now().minusSeconds(1), null);
    when(codeRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc("user@example.com"))
        .thenReturn(Optional.of(recovery));

    service.confirm("user@example.com", "123456", "new-password");

    verify(usuarioRepository, never()).save(any());
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  void usedCodeDoesNotChangePassword() {
    PasswordRecoveryCode recovery = code(Instant.now().plusSeconds(60), Instant.now());
    when(codeRepository.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc("user@example.com"))
        .thenReturn(Optional.of(recovery));

    service.confirm("user@example.com", "123456", "new-password");

    verify(usuarioRepository, never()).save(any());
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  private Usuario activeUser() {
    Usuario user = new Usuario();
    user.setActivo(true);
    user.setEmail("user@example.com");
    return user;
  }

  private PasswordRecoveryCode code(Instant expiresAt, Instant usedAt) {
    PasswordRecoveryCode recovery = new PasswordRecoveryCode();
    recovery.setCodeHash("hash");
    recovery.setExpiresAt(expiresAt);
    recovery.setUsedAt(usedAt);
    recovery.setAttempts(0);
    return recovery;
  }
}
