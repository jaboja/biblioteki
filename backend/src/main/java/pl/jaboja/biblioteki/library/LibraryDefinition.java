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
        null,
        LibrarySystemType.PRIMO
    ),
    DBP(
        "Dolnośląska Biblioteka Publiczna (Wrocław)",
        "https://omnis-dbp.primo.exlibrisgroup.com",
        "48OMNIS_WBP:WBP",
        true,
        "Rynek",
        LibrarySystemType.PRIMO
    ),
    ZNO(
        "Biblioteka Ossolineum (Wrocław)",
        "https://omnis-zno.primo.exlibrisgroup.com",
        "48OMNIS_ZNO:ZNO",
        false,
        null,
        LibrarySystemType.PRIMO
    ),
    WIMBP_GORZOW(
        "Wojewódzka i Miejska Biblioteka Publiczna (Gorzów Wielkopolski)",
        "https://opac.wimbp.gorzow.pl/integro",
        null,
        false,
        null,
        LibrarySystemType.INTEGRO
    );

    private final String displayName;
    private final String baseUrl;
    private final String vid;
    /** Next Discovery Experience – wymaga dodatkowego nagłówka is-nde: true */
    private final boolean nde;
    private final String location;
    private final LibrarySystemType systemType;

    /** Kod instytucji – pierwsza część vid przed ':' */
    public String getInstCode() {
        return vid != null ? vid.split(":")[0] : null;
    }
}
