package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.model.VendaSuspensa;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import ao.co.hzconsultoria.efacturacao.repository.VendaSuspensaRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendas-suspensas")
public class VendaSuspensaController {

    @Autowired
    private VendaSuspensaRepository repository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> suspender(@RequestBody Map<String, Object> payload) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Long userId = SecurityUtils.getCurrentUserId();

        System.out.println("Suspender venda - Empresa: " + empresaId + ", Usuário: " + userId);

        VendaSuspensa venda = new VendaSuspensa();
        String cliNome = (String) payload.get("clienteNome");
        venda.setClienteNome((cliNome != null && !cliNome.trim().isEmpty()) ? cliNome : "Consumidor Final");
        venda.setItensJson((String) payload.get("itensJson"));

        if (empresaId != null) {
            venda.setEmpresa(empresaRepository.findById(empresaId).orElse(null));
        }
        if (venda.getEmpresa() == null) {
            empresaRepository.findAll().stream().findFirst().ifPresent(venda::setEmpresa);
        }

        if (userId != null) {
            userRepository.findById(userId).ifPresent(venda::setOperador);
        }
        if (venda.getOperador() == null) {
            userRepository.findByLogin("admin").ifPresent(venda::setOperador);
        }

        VendaSuspensa salva = repository.save(venda);
        System.out.println("Venda suspensa salva com ID: " + salva.getId());

        return ResponseEntity.ok().body("Venda suspensa com sucesso!");
    }

    @GetMapping
    public List<VendaSuspensa> listar() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId != null) {
            List<VendaSuspensa> lista = repository.findByEmpresa_IdOrderByDataHoraDesc(empresaId);
            if (!lista.isEmpty()) {
                return lista;
            }
        }
        return repository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dataHora"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (id != null && repository.existsById(id)) {
            repository.deleteById(id);
        }
        return ResponseEntity.ok().build();
    }
}
