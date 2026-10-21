package dk.campusbornholm.services;

import dk.campusbornholm.exceptions.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WordleServiceTest {
    @Nested
    @DisplayName("evaluate – vurdering af et gæt")
    class Evaluate {
        @Test
        @DisplayName("Rigtigt gæt giver 5 grønne")
        void allCorrect() {
            assertEquals("GGGGG", WordleService.evaluate("kaffe", "kaffe"));
        }

        @Test
        @DisplayName("Ingen fælles bogstaver giver 5 grå")
        void noneCorrect() {
            assertEquals("XXXXX", WordleService.evaluate("bliss", "kaffe"));
        }

        @Test
        @DisplayName("Rigtige bogstaver på rigtig plads bliver grønne")
        void someGreen() {
            assertEquals("GGXXG", WordleService.evaluate("kaste", "kaffe"));
        }

        @Test
        @DisplayName("Rigtige bogstaver på forkert plads bliver gule")
        void yellows() {
            assertEquals("YGYXG", WordleService.evaluate("fakke", "kaffe"));
        }

        @Test
        @DisplayName("Dobbeltbogstav i gættet, men kun ét i svaret: kun det første bliver gult")
        void duplicateInGuessOnlyOneInAnswer() {
            assertEquals("YYXYX", WordleService.evaluate("otter", "hoste"));
        }

        @Test
        @DisplayName("Grøn har forrang: et bogstav brugt som grønt kan ikke også give gult")
        void greenConsumesLetter() {
            assertEquals("XGXXX", WordleService.evaluate("aaaaa", "maler"));
        }

        @Test
        @DisplayName("Grøn har forrang, også når det grønne bogstav står sidst")
        void greenAtEndConsumesLetter() {
            assertEquals("XXXXG", WordleService.evaluate("eeeee", "hoste"));
        }

        @Test
        @DisplayName("Danske bogstaver (æ, ø, å) virker")
        void danishLetters() {
            assertEquals("YGXGG", WordleService.evaluate("bøtte", "købte"));
        }

        @Test
        @DisplayName("Store og små bogstaver er ligegyldige")
        void caseInsensitive() {
            assertEquals("GGGGG", WordleService.evaluate("KAFFE", "kaffe"));
        }

        @ParameterizedTest(name = "gæt \"{0}\" mod svar \"{1}\" giver {2}")
        @DisplayName("Flere kombinationer")
        @CsvSource({
                "kaffe, kaffe, GGGGG",
                "kaste, kaffe, GGXXG",
                "fakke, kaffe, YGYXG",
                "otter, hoste, YYXYX",
                "aaaaa, maler, XGXXX",
                "maler, remal, YYYYY",
                "bøtte, købte, YGXGG"
        })
        void combinations(String guess, String answer, String expected) {
            assertEquals(expected, WordleService.evaluate(guess, answer));
        }
    }

    @Nested
    @DisplayName("normalize – validering af et gæt (US13)")
    class Normalize {
        @Test
        @DisplayName("Mellemrum fjernes og store bogstaver bliver små")
        void trimsAndLowercases() {
            assertEquals("kaffe", WordleService.normalize("  KaFfE "));
        }

        @ParameterizedTest(name = "\"{0}\" afvises med 400")
        @DisplayName("Ugyldige gæt afvises")
        @ValueSource(strings = {"", "     ", "kaf", "kaffer", "kaf3e", "ka fe", "kaf-e"})
        void invalidGuesses(String guess) {
            ApiException e = assertThrows(ApiException.class, () -> WordleService.normalize(guess));
            assertEquals(400, e.getStatusCode());
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("null afvises med 400")
        void nullGuess(String guess) {
            ApiException e = assertThrows(ApiException.class, () -> WordleService.normalize(guess));
            assertEquals(400, e.getStatusCode());
        }
    }
}
