package pl.jaboja.biblioteki.library;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LibraryDefinition {

    MBP(
        "Miejska Biblioteka Publiczna (Wrocław)",
        "https://omnis-mbpwr.primo.exlibrisgroup.com",
        "48OMNIS_MBP:MBP",
        false,
        null
    ),
    DBP(
        "Dolnośląska Biblioteka Publiczna (Wrocław)",
        "https://omnis-dbp.primo.exlibrisgroup.com",
        "48OMNIS_WBP:WBP",
        true,
        "Rynek"
    ),
    ZNO(
        "Biblioteka Ossolineum (Wrocław)",
        "https://omnis-zno.primo.exlibrisgroup.com",
        "48OMNIS_ZNO:ZNO",
        false,
        null
    );

    private final String displayName;
    private final String baseUrl;
    private final String vid;
    /** Next Discovery Experience – wymaga dodatkowego nagłówka is-nde: true */
    private final boolean nde;
    private final String location;

    /** Kod instytucji – pierwsza część vid przed ':' */
    public String getInstCode() {
        return vid.split(":")[0];
    }
}
