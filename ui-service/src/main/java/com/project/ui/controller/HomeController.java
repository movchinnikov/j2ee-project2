package com.project.ui.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Value("${services.iac-url}")
    private String iacUrl;

    @Value("${services.property-url}")
    private String propertyUrl;

    @Value("${services.pricing-url}")
    private String pricingUrl;

    @Value("${services.order-url}")
    private String orderUrl;

    private void addUrls(Model model) {
        model.addAttribute("iacUrl", iacUrl);
        model.addAttribute("propertyUrl", propertyUrl);
        model.addAttribute("pricingUrl", pricingUrl);
        model.addAttribute("orderUrl", orderUrl);
    }

    @GetMapping("/")
    public String home() { return "redirect:/login"; }

    @GetMapping("/login")
    public String login(Model model) { addUrls(model); return "login"; }

    @GetMapping("/register")
    public String register(Model model) { addUrls(model); return "register"; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) { addUrls(model); return "dashboard"; }
}
