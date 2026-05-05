package org.bytewright.bgmo.domain.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InputSanitizer {
  private final SiteManagementService siteManagementService;

  public String plainText(String input) {
    if (input == null) return null;
    String baseUrl = siteManagementService.getBaseUrl().toString();
    return Jsoup.clean(input, baseUrl, Safelist.none(), new OutputSettings().prettyPrint(false));
  }
}
