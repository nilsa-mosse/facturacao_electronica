package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Inventario;
import ao.co.hzconsultoria.efacturacao.model.ItemInventario;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.InventarioRepository;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.repository.EstabelecimentoRepository;
import ao.co.hzconsultoria.efacturacao.service.StockService;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestao-inventarios")
public class InventarioController {

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;

    @GetMapping
    public String listar(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("inventarios", inventarioRepository.findByEmpresa_IdOrderByCreatedAtDesc(empresaId));
        return "listarInventarios";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Inventario inventario = new Inventario();
        
        // Gerar código automático: INV-ANO-001
        int anoAtual = java.time.LocalDate.now().getYear();
        long totalInventariosAno = inventarioRepository.findByEmpresa_Id(empresaId).stream()
                .filter(i -> i.getCreatedAt().getYear() == anoAtual)
                .count();
        
        String codigoGerado = String.format("INV-%d-%03d", anoAtual, totalInventariosAno + 1);
        inventario.setCodigo(codigoGerado);
        inventario.setDataAbertura(java.time.LocalDate.now());
        inventario.setEstado(Inventario.EstadoInventario.RASCUNHO);
        
        model.addAttribute("inventario", inventario);
        model.addAttribute("produtosDisponiveis", stockService.listarTodosProdutos());
        model.addAttribute("categorias", categoriaRepository.findAll());
        // Listar estabelecimentos (armazéns) associados à empresa atual
        if (empresaId != null) {
            model.addAttribute("armazens", estabelecimentoRepository.findByEmpresa_Id(empresaId));
        } else {
            model.addAttribute("armazens", java.util.Collections.emptyList());
        }
        model.addAttribute("dataAtual", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        ao.co.hzconsultoria.efacturacao.security.CustomUserDetails user = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentUser();
        model.addAttribute("usuarioAtual", user != null ? user.getNome() : "Admin");
        return "cadastroInventario";
    }



    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Inventario inventario, 
                        @RequestParam(value = "itemProdutoIds", required = false) java.util.List<Long> itemProdutoIds,
                        @RequestParam(value = "itemQuantidadesContadas", required = false) java.util.List<Double> itemQuantidadesContadas,
                        RedirectAttributes ra) {
        
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId != null) {
            Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
            inventario.setEmpresa(empresa);
        }
        
        try {
            // Processar Itens do Inventário
            if (itemProdutoIds != null && !itemProdutoIds.isEmpty()) {
                java.util.List<ItemInventario> itens = new java.util.ArrayList<>();
                for (int i = 0; i < itemProdutoIds.size(); i++) {
                    Long prodId = itemProdutoIds.get(i);
                    Double qtdContada = itemQuantidadesContadas.get(i);
                    
                    Produto produto = produtoRepository.findById(prodId).orElse(null);
                    if (produto != null) {
                        ItemInventario item = new ItemInventario();
                        item.setInventario(inventario);
                        item.setProduto(produto);
                        item.setQuantidadeSistema(produto.getQuantidadeEstoque());
                        item.setQuantidadeContada(qtdContada);
                        itens.add(item);
                    }
                }
                inventario.setItens(itens);
            }

            inventarioRepository.save(inventario);
            ra.addFlashAttribute("mensagemSucesso", "Inventário guardado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("mensagemErro", "Erro ao guardar inventário: " + e.getMessage());
            e.printStackTrace();
        }
        
        return "redirect:/gestao-inventarios";
    }

    @GetMapping("/iniciar/{id}")
    public String iniciar(@PathVariable Long id, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv != null && inv.getEstado() == Inventario.EstadoInventario.RASCUNHO) {
            inv.setEstado(Inventario.EstadoInventario.EM_CONTAGEM);
            inventarioRepository.save(inv);
            ra.addFlashAttribute("mensagemSucesso", "Contagem iniciada!");
        }
        return "redirect:/gestao-inventarios";
    }

    @GetMapping("/fechar/{id}")
    public String fechar(@PathVariable Long id, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv != null && inv.getEstado() == Inventario.EstadoInventario.EM_CONTAGEM) {
            // Antes de fechar, aplicar ajustes de stock com base nas divergências dos itens
            try {
                if (inv.getItens() != null) {
                    for (ao.co.hzconsultoria.efacturacao.model.ItemInventario item : inv.getItens()) {
                        if (item == null || item.getProduto() == null) continue;
                        double divergencia = item.getDivergencia() != null ? item.getDivergencia() : 0.0;
                        if (Math.abs(divergencia) > 0.0001) {
                            String tipo = divergencia > 0 ? "ENTRA" : "SAIDA";
                            Double quantidade = Math.abs(divergencia);
                            String motivo = "Ajuste por Fecho de Inventário";
                            String referencia = inv.getCodigo() != null ? inv.getCodigo() : ("INV-" + inv.getId());
                            String origem = "INVENTARIO";
                            String nomeDoc = referencia;
                            byte[] blobDoc = null;
                            Double precoCusto = item.getProduto() != null ? item.getProduto().getPrecoCompra() : null;

                            movimentoSucesso:
                            try {
                                stockService.registrarMovimento(item.getProduto().getId(), quantidade, tipo, motivo,
                                        referencia, origem, nomeDoc, blobDoc, precoCusto);
                            } catch (Exception e) {
                                // Log and continue with other items
                                e.printStackTrace();
                                ra.addFlashAttribute("mensagemErro", "Erro ao registar ajuste de stock para produto " + item.getProduto().getNome() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                ra.addFlashAttribute("mensagemErro", "Erro ao aplicar ajustes de stock: " + ex.getMessage());
                return "redirect:/gestao-inventarios";
            }

            inv.setEstado(Inventario.EstadoInventario.EM_REVISAO);
            inventarioRepository.save(inv);
            ra.addFlashAttribute("mensagemSucesso", "Inventário enviado para revisão e stock actualizado!");
        }
        return "redirect:/gestao-inventarios";
    }

    @GetMapping("/aprovar/{id}")
    public String aprovar(@PathVariable Long id, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv != null && inv.getEstado() == Inventario.EstadoInventario.EM_REVISAO) {
            inv.setEstado(Inventario.EstadoInventario.FINALIZADO);
            // Aqui futuramente entraria a lógica de ajuste de stock real
            inventarioRepository.save(inv);
            ra.addFlashAttribute("mensagemSucesso", "Inventário finalizado e stock ajustado!");
        }
        return "redirect:/gestao-inventarios";
    }

    @GetMapping("/cancelar/{id}")
    public String cancelar(@PathVariable Long id, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv != null && inv.getEstado() != Inventario.EstadoInventario.FINALIZADO) {
            inv.setEstado(Inventario.EstadoInventario.CANCELADO);
            inventarioRepository.save(inv);
            ra.addFlashAttribute("mensagemSucesso", "Inventário cancelado.");
        }
        return "redirect:/gestao-inventarios";
    }

    @GetMapping("/contar/{id}")
    public String contar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv == null) {
            ra.addFlashAttribute("mensagemErro", "Inventário não encontrado.");
            return "redirect:/gestao-inventarios";
        }

        // Ensure the inventory is in a state that allows counting (optional)
        if (inv.getEstado() != Inventario.EstadoInventario.EM_CONTAGEM && inv.getEstado() != Inventario.EstadoInventario.RASCUNHO) {
            ra.addFlashAttribute("mensagemErro", "Este inventário não está disponível para contagem.");
            return "redirect:/gestao-inventarios";
        }

        model.addAttribute("inventario", inv);
        model.addAttribute("itens", inv.getItens());
        model.addAttribute("produtosDisponiveis", stockService.listarTodosProdutos());
        ao.co.hzconsultoria.efacturacao.security.CustomUserDetails user = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentUser();
        model.addAttribute("usuarioAtual", user != null ? user.getNome() : "Operador");
        return "contarInventario";
    }

    @GetMapping("/detalhes/{id}")
    public String detalhes(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv == null) {
            ra.addFlashAttribute("mensagemErro", "Inventário não encontrado.");
            return "redirect:/gestao-inventarios";
        }

        model.addAttribute("inventario", inv);
        model.addAttribute("itens", inv.getItens());
        ao.co.hzconsultoria.efacturacao.security.CustomUserDetails user = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentUser();
        model.addAttribute("usuarioAtual", user != null ? user.getNome() : "Usuário");
        return "detalhesInventario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv == null) {
            ra.addFlashAttribute("mensagemErro", "Inventário não encontrado.");
            return "redirect:/gestao-inventarios";
        }

        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("inventario", inv);
        model.addAttribute("produtosDisponiveis", stockService.listarTodosProdutos());
        model.addAttribute("categorias", categoriaRepository.findAll());
        if (empresaId != null) {
            model.addAttribute("armazens", estabelecimentoRepository.findByEmpresa_Id(empresaId));
        } else {
            model.addAttribute("armazens", java.util.Collections.emptyList());
        }
        model.addAttribute("dataAtual", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        ao.co.hzconsultoria.efacturacao.security.CustomUserDetails user = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentUser();
        model.addAttribute("usuarioAtual", user != null ? user.getNome() : "Admin");

        // Ensure items are present on the model for step 3 if needed
        model.addAttribute("itens", inv.getItens());

        return "cadastroInventario";
    }

    @GetMapping("/editar")
    public String editarSemId(RedirectAttributes ra) {
        ra.addFlashAttribute("mensagemErro", "ID do inventário não foi fornecido.");
        return "redirect:/gestao-inventarios";
    }
}
