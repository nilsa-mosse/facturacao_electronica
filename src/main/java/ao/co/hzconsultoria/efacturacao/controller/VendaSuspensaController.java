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

        VendaSuspensa venda = new VendaSuspensa();
        venda.setClienteNome((String) payload.get("clienteNome"));
        venda.setItensJson((String) payload.get("itensJson"));
        
        venda.setEmpresa(empresaRepository.findById(empresaId).orElse(null));
        venda.setOperador(userRepository.findById(userId).orElse(null));

        repository.save(venda);
        return ResponseEntity.ok().body("Venda suspensa com sucesso!");
    }

    @GetMapping
    public List<VendaSuspensa> listar() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        return repository.findByEmpresa_IdOrderByDataHoraDesc(empresaId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
