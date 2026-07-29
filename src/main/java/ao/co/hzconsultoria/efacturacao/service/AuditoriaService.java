package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Auditoria;
import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.repository.AuditoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.time.LocalDateTime;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private UserRepository userRepository;

    public void registar(String acao, String entidade, String entidadeId, String valorAnterior, String valorNovo, String detalhes) {
        try {
            Auditoria audit = new Auditoria();
            audit.setDataHora(LocalDateTime.now());
            audit.setAcao(acao);
            audit.setEntidade(entidade);
            audit.setEntidadeId(entidadeId);
            audit.setValorAnterior(valorAnterior);
            audit.setValorNovo(valorNovo);
            audit.setDetalhes(detalhes);

            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            audit.setEmpresaId(empresaId);

            Long userId = SecurityUtils.getCurrentUserId();
            if (userId != null) {
                userRepository.findById(userId).ifPresent(user -> {
                    audit.setUsuario(user);
                    audit.setNomeUsuario(user.getNome());
                    audit.setEmailUsuario(user.getEmail());
                });
            } else {
                audit.setNomeUsuario("SISTEMA / ANÓNIMO");
            }

            // Captura dados da requisição HTTP (IP e Hostname do Computador)
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = extrairClientIp(request);
                audit.setIpCliente(ip);
                audit.setNomeComputador(extrairHostName(ip));
            } else {
                audit.setIpCliente("127.0.0.1");
                audit.setNomeComputador("LOCAL_HOST");
            }

            auditoriaRepository.save(audit);
        } catch (Exception e) {
            System.err.println("Erro ao gravar registo de auditoria: " + e.getMessage());
        }
    }

    public Page<Auditoria> listarPorEmpresa(Long empresaId, int page, int size) {
        return auditoriaRepository.findByEmpresaIdOrderByDataHoraDesc(empresaId, PageRequest.of(page, size));
    }

    private String extrairClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "127.0.0.1";
    }

    private String extrairHostName(String ip) {
        try {
            if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                return InetAddress.getLocalHost().getHostName();
            }
            InetAddress addr = InetAddress.getByName(ip);
            return addr.getHostName();
        } catch (Exception e) {
            return "PC-" + ip;
        }
    }
}
