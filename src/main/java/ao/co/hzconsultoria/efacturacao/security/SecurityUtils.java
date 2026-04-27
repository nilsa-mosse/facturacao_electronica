package ao.co.hzconsultoria.efacturacao.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getCurrentEmpresaId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getEmpresaId();
        }
        return null;
    }

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getId();
        }
        return null;
    }

    /**
     * Obtém a empresa do utilizador autenticado
     * Útil para garantir que cada utilizador acessa apenas a sua empresa
     */
    public static CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) auth.getPrincipal();
        }
        return null;
    }

    /**
     * Verifica se o utilizador autenticado tem acesso à empresa especificada
     * Útil para validações de segurança
     */
    public static boolean temAcessoEmpresa(Long empresaId) {
        Long empresaIdAtual = getCurrentEmpresaId();
        return empresaIdAtual != null && empresaIdAtual.equals(empresaId);
    }
}

