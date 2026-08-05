package com.visium.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visium.backend.dto.usuario.UsuarioRequest;
import com.visium.backend.dto.usuario.UsuarioResponse;
import com.visium.backend.entity.Empresa;
import com.visium.backend.entity.Rol;
import com.visium.backend.entity.Sucursal;
import com.visium.backend.entity.Usuario;
import com.visium.backend.entity.UsuarioEmpresa;
import com.visium.backend.exception.BadRequestException;
import com.visium.backend.exception.ForbiddenException;
import com.visium.backend.exception.ResourceNotFoundException;
import com.visium.backend.repository.EmpresaRepository;
import com.visium.backend.repository.RolRepository;
import com.visium.backend.repository.SucursalRepository;
import com.visium.backend.repository.UsuarioEmpresaRolRepository;
import com.visium.backend.repository.UsuarioEmpresaRepository;
import com.visium.backend.repository.UsuarioRepository;
import com.visium.backend.repository.UsuarioSucursalRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

	private static final UUID EMPRESA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID SUCURSAL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID USUARIO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private EmpresaRepository empresaRepository;

	@Mock
	private SucursalRepository sucursalRepository;

	@Mock
	private RolRepository rolRepository;

	@Mock
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Mock
	private UsuarioEmpresaRolRepository usuarioEmpresaRolRepository;

	@Mock
	private UsuarioSucursalRepository usuarioSucursalRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AccesoService accesoService;

	private UsuarioService usuarioService;

	@BeforeEach
	void setUp() {
		usuarioService = new UsuarioService(
				usuarioRepository,
				empresaRepository,
				sucursalRepository,
				rolRepository,
				usuarioEmpresaRepository,
				usuarioEmpresaRolRepository,
				usuarioSucursalRepository,
				passwordEncoder,
				accesoService);
	}

	@Test
	void crearUsuarioSinRolExitoso() {
		when(accesoService.resolverEmpresaObjetivo(EMPRESA_ID)).thenReturn(EMPRESA_ID);
		when(usuarioRepository.findByEmailIgnoreCase("test@empresa.com")).thenReturn(Optional.empty());
		when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa()));
		when(passwordEncoder.encode("clave123")).thenReturn("hashed");
		when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(usuarioEmpresaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(usuarioEmpresaRolRepository.findByUsuarioEmpresaId(any())).thenReturn(List.of());
		when(usuarioSucursalRepository.findByUsuarioEmpresaId(any())).thenReturn(List.of());

		UsuarioRequest request = request(EMPRESA_ID, null, List.of());
		UsuarioResponse response = usuarioService.crear(request);

		assertEquals("test@empresa.com", response.getEmail());
		assertTrue(response.getActivo());
	}

	@Test
	void crearUsuarioConRolJefeExitoso() {
		when(accesoService.resolverEmpresaObjetivo(EMPRESA_ID)).thenReturn(EMPRESA_ID);
		when(usuarioRepository.findByEmailIgnoreCase("test@empresa.com")).thenReturn(Optional.empty());
		when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa()));
		when(rolRepository.findByCodigo("JEFE")).thenReturn(Optional.of(rol("JEFE")));
		when(passwordEncoder.encode("clave123")).thenReturn("hashed");
		when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(usuarioEmpresaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(usuarioEmpresaRolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal()));
		doAnswer(inv -> null).when(accesoService).exigirAccesoSucursal(EMPRESA_ID, SUCURSAL_ID);
		when(usuarioSucursalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(usuarioEmpresaRolRepository.findByUsuarioEmpresaId(any())).thenReturn(List.of());
		when(usuarioSucursalRepository.findByUsuarioEmpresaId(any())).thenReturn(List.of());

		UsuarioRequest request = request(EMPRESA_ID, "JEFE", List.of(SUCURSAL_ID));
		UsuarioResponse response = usuarioService.crear(request);

		verify(usuarioEmpresaRolRepository).save(any());
	}

	@Test
	void crearConRolSuperAdminSinSerSuperAdminLanzaForbidden() {
		when(accesoService.resolverEmpresaObjetivo(EMPRESA_ID)).thenReturn(EMPRESA_ID);
		when(accesoService.esSuperAdmin()).thenReturn(false);

		UsuarioRequest request = request(EMPRESA_ID, "SUPER_ADMIN", List.of());
		assertThrows(ForbiddenException.class, () -> usuarioService.crear(request));
	}

	@Test
	void crearConRolInvalidoLanzaBadRequest() {
		when(accesoService.resolverEmpresaObjetivo(EMPRESA_ID)).thenReturn(EMPRESA_ID);

		UsuarioRequest request = request(EMPRESA_ID, "ROL_INVENTADO", List.of());
		assertThrows(BadRequestException.class, () -> usuarioService.crear(request));
	}

	@Test
	void crearEmailDuplicadoLanzaBadRequest() {
		when(accesoService.resolverEmpresaObjetivo(EMPRESA_ID)).thenReturn(EMPRESA_ID);
		Usuario existing = usuario();
		existing.setId(USUARIO_ID);
		when(usuarioRepository.findByEmailIgnoreCase("dup@empresa.com")).thenReturn(Optional.of(existing));
		when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa()));

		UsuarioRequest request = request(EMPRESA_ID, null, List.of());
		request.setEmail("dup@empresa.com");
		assertThrows(BadRequestException.class, () -> usuarioService.crear(request));
	}

	@Test
	void crearPasswordObligatoriaLanzaBadRequest() {
		when(accesoService.resolverEmpresaObjetivo(EMPRESA_ID)).thenReturn(EMPRESA_ID);
		when(usuarioRepository.findByEmailIgnoreCase("nopass@empresa.com")).thenReturn(Optional.empty());
		when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa()));

		UsuarioRequest request = request(EMPRESA_ID, null, List.of());
		request.setEmail("nopass@empresa.com");
		request.setPassword(null);
		assertThrows(BadRequestException.class, () -> usuarioService.crear(request));
	}

	@Test
	void editarUsuarioExitoso() {
		UUID empresaId = EMPRESA_ID;
		Usuario usuario = usuario();
		usuario.setId(USUARIO_ID);
		UsuarioEmpresa pertenencia = pertenencia(usuario, empresaId);
		pertenencia.setActivo(true);

		when(accesoService.resolverEmpresaObjetivo(empresaId)).thenReturn(empresaId);
		when(usuarioEmpresaRepository.findByUsuarioIdAndEmpresaId(USUARIO_ID, empresaId))
				.thenReturn(Optional.of(pertenencia));
		when(usuarioRepository.findByEmailIgnoreCase("nuevo@empresa.com")).thenReturn(Optional.empty());
		when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa()));
		when(rolRepository.findByCodigo("JEFE")).thenReturn(Optional.of(rol("JEFE")));
		when(passwordEncoder.encode("nuevaclave")).thenReturn("hashed2");
		when(usuarioEmpresaRolRepository.findByUsuarioEmpresaId(pertenencia.getId()))
				.thenReturn(List.of());
		when(usuarioSucursalRepository.findByUsuarioEmpresaId(pertenencia.getId()))
				.thenReturn(List.of());
		when(usuarioEmpresaRolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(usuarioEmpresaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(usuarioSucursalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(sucursalRepository.findById(SUCURSAL_ID)).thenReturn(Optional.of(sucursal()));
		doAnswer(inv -> null).when(accesoService).exigirAccesoSucursal(empresaId, SUCURSAL_ID);

		UsuarioRequest request = request(empresaId, "JEFE", List.of(SUCURSAL_ID));
		request.setPassword("nuevaclave");
		request.setEmail("nuevo@empresa.com");
		UsuarioResponse response = usuarioService.editar(USUARIO_ID, request);

		assertEquals("nuevo@empresa.com", response.getEmail());
		verify(usuarioEmpresaRolRepository).deleteByUsuarioEmpresaId(pertenencia.getId());
		verify(usuarioEmpresaRolRepository).save(any());
	}

	@Test
	void cambiarEstadoDesactivaUsuarioYPertenencia() {
		Usuario usuario = usuario();
		usuario.setId(USUARIO_ID);
		usuario.setActivo(true);
		UsuarioEmpresa pertenencia = pertenencia(usuario, EMPRESA_ID);
		pertenencia.setActivo(true);

		when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
		when(usuarioEmpresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(List.of(pertenencia));
		when(accesoService.puedeAccederEmpresa(EMPRESA_ID)).thenReturn(true);
		when(usuarioEmpresaRepository.save(pertenencia)).thenReturn(pertenencia);
		when(usuarioRepository.save(usuario)).thenReturn(usuario);

		usuarioService.cambiarEstado(USUARIO_ID, false);

		assertFalse(usuario.getActivo());
		assertFalse(pertenencia.getActivo());
	}

	@Test
	void cambiarEstadoSinAccesoLanzaForbidden() {
		Usuario usuario = usuario();
		usuario.setId(USUARIO_ID);
		UsuarioEmpresa pertenencia = pertenencia(usuario, EMPRESA_ID);
		pertenencia.setActivo(true);

		when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
		when(usuarioEmpresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(List.of(pertenencia));
		when(accesoService.puedeAccederEmpresa(EMPRESA_ID)).thenReturn(false);

		assertThrows(ForbiddenException.class, () -> usuarioService.cambiarEstado(USUARIO_ID, false));
	}

	@Test
	void listarFiltraPorVisibilidad() {
		Usuario usuario = usuario();
		usuario.setId(USUARIO_ID);
		UsuarioEmpresa pertenencia = pertenencia(usuario, EMPRESA_ID);
		pertenencia.setActivo(true);

		when(usuarioRepository.findAll()).thenReturn(List.of(usuario));
		when(usuarioEmpresaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(List.of(pertenencia));
		when(accesoService.puedeAccederEmpresa(EMPRESA_ID)).thenReturn(true);
		when(accesoService.sucursalIdsVisiblesEnEmpresa()).thenReturn(List.of());
		when(usuarioEmpresaRolRepository.findByUsuarioEmpresaId(any()))
				.thenReturn(List.of());
		when(usuarioSucursalRepository.findByUsuarioEmpresaId(any()))
				.thenReturn(List.of());

		List<UsuarioResponse> lista = usuarioService.listar();
		assertFalse(lista.isEmpty());
	}

	private UsuarioRequest request(UUID empresaId, String rol, List<UUID> sucursalIds) {
		UsuarioRequest req = new UsuarioRequest();
		req.setEmpresaId(empresaId);
		req.setNombre("Test");
		req.setApellido("User");
		req.setEmail("test@empresa.com");
		req.setPassword("clave123");
		req.setRun("12345678");
		req.setTelefono("+56912345678");
		req.setRol(rol);
		req.setSucursalIds(sucursalIds);
		return req;
	}

	private Empresa empresa() {
		Empresa e = new Empresa();
		e.setId(EMPRESA_ID);
		return e;
	}

	private Sucursal sucursal() {
		Sucursal s = new Sucursal();
		s.setId(SUCURSAL_ID);
		s.setEmpresa(empresa());
		return s;
	}

	private Usuario usuario() {
		Usuario u = new Usuario();
		u.setId(USUARIO_ID);
		u.setNombre("Test");
		u.setApellido("User");
		u.setEmail("test@empresa.com");
		u.setActivo(true);
		return u;
	}

	private UsuarioEmpresa pertenencia(Usuario usuario, UUID empresaId) {
		UsuarioEmpresa p = new UsuarioEmpresa();
		p.setId(UUID.randomUUID());
		p.setUsuario(usuario);
		p.setEmpresa(empresa());
		p.setActivo(true);
		return p;
	}

	private Rol rol(String codigo) {
		Rol r = new Rol();
		r.setId((short) 1);
		r.setCodigo(codigo);
		return r;
	}
}