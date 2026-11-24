package com.example.cashly_backend.provider;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.cashly_backend.dto.CouponResponse;

/**
 * Cupons de exemplo usados enquanto nenhuma API de afiliado estiver
 * configurada (ou quando ela estiver indisponível).
 */
@Component
public class DemoCouponProvider implements CouponProvider {

    @Override
    public boolean isFallback() {
        return true;
    }

    @Override
    public List<CouponResponse> fetch() {
        LocalDateTime now = LocalDateTime.now();

        return List.of(
                coupon(1, "Magalu", "R$ 50 OFF em compras acima de R$ 299",
                        "Válido para produtos selecionados no site. Não cumulativo com outras promoções.",
                        "CASHLY50", "R$ 50 OFF", "Eletrônicos", "https://www.magalu.com/",
                        now.plusDays(5), true, 1240),
                coupon(2, "Amazon", "Até 40% OFF em produtos selecionados",
                        "Desconto aplicado no checkout em itens participantes.",
                        "CASHLY40", "40% OFF", "Eletrônicos", "https://www.amazon.com.br/",
                        now.plusDays(3), true, 980),
                coupon(3, "Renner", "20% OFF na primeira compra",
                        "Para clientes novos no site ou aplicativo.",
                        "BEMVINDO20", "20% OFF", "Moda", "https://www.renner.com.br/",
                        now.plusDays(7), false, 645),
                coupon(4, "Nike", "30% OFF em tênis selecionados",
                        "Cupom válido para a linha de corrida.",
                        "CORRIDA30", "30% OFF", "Moda", "https://www.nike.com.br/",
                        now.plusDays(2), true, 2110),
                coupon(5, "iFood", "R$ 25 OFF no seu primeiro pedido",
                        "Pedido mínimo de R$ 40 em restaurantes participantes.",
                        "PRIMEIRO25", "R$ 25 OFF", "Alimentação", "https://www.ifood.com.br/",
                        now.plusDays(10), false, 3320),
                coupon(6, "Booking.com", "15% OFF em hotéis selecionados",
                        "Reservas com cancelamento gratuito inclusas.",
                        "VIAJAR15", "15% OFF", "Viagem", "https://www.booking.com/",
                        now.plusDays(12), false, 430),
                coupon(7, "Sephora", "15% OFF em perfumaria",
                        "Válido para marcas selecionadas, enquanto durar o estoque.",
                        "BELEZA15", "15% OFF", "Beleza", "https://www.sephora.com.br/",
                        now.plusDays(6), false, 870),
                coupon(8, "Samsung", "10% OFF em smartphones",
                        "Desconto em modelos Galaxy selecionados.",
                        "GALAXY10", "10% OFF", "Eletrônicos", "https://www.samsung.com/br/",
                        now.plusDays(4), false, 1560),
                coupon(9, "MadeiraMadeira", "R$ 100 OFF em móveis",
                        "Compras acima de R$ 799 em móveis e decoração.",
                        "CASA100", "R$ 100 OFF", "Casa", "https://www.madeiramadeira.com.br/",
                        now.plusDays(8), false, 390),
                coupon(10, "Grupo Boticário", "20% OFF no site todo",
                        "Exceto kits promocionais e presentes.",
                        "BOTI20", "20% OFF", "Beleza", "https://www.boticario.com.br/",
                        now.plusDays(1), true, 1780));
    }

    private static CouponResponse coupon(int id, String store, String title, String description, String code,
            String discount, String category, String affiliateUrl, LocalDateTime expiresAt, boolean featured,
            int uses) {
        return new CouponResponse(
                id,
                "demo-" + id,
                store,
                title,
                description,
                code,
                discount,
                category,
                null,
                affiliateUrl,
                expiresAt,
                uses,
                featured,
                "demo");
    }
}
