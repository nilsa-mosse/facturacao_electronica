package ao.co.hzconsultoria.efacturacao.security;

import ao.co.hzconsultoria.efacturacao.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user.getRole();
        if (role != null && !role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return user.getSenha();
    }

    @Override
    public String getUsername() {
        return user.getLogin();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { 
        if (user.getBloqueadoAte() == null) return true;
        return LocalDateTime.now().isAfter(user.getBloqueadoAte());
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() { return user.isAtivo(); }

    public String getNome() { return user.getNome(); }
    public String getRole() { return user.getRole(); }
    public Long getEmpresaId() { 
        return (user.getEmpresa() != null) ? user.getEmpresa().getId() : null; 
    }
}
