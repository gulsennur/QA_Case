import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CheckPrice {
    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        driver.get("https://getir.com/en");
        System.out.println("Getir web page was opened successfully.");

        try {
            // Cookie banner'ı varsa kapat
            try {
                WebElement cookieAcceptButton = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("[data-testid='cookie-permission'] button")));
                cookieAcceptButton.click();
                System.out.println("Cookie banner closed.");
            } catch (Exception e) {
                System.out.println("Cookie banner not displayed, continuing.");
            }

            // Fit&Form kategorisine tıkla
            WebElement fitFormSpan = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span[@type='primary' and contains(text(),'Fit')]")));
            WebElement fitFormLink = fitFormSpan.findElement(By.xpath("./ancestor::a"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", fitFormLink);
            Thread.sleep(1000);
            fitFormLink.click();
            wait.until(ExpectedConditions.urlContains("fit-form"));
            System.out.println("Redirected to the Fit&Form category.");

            // Granola alt kategorisini aç
            WebElement granolaLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("a[href='/en/category/fit-form-GEn6GVDJ1d/58344853e2dfa9000401476a/']")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", granolaLink);
            granolaLink.click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h5[@data-testid='title' and contains(text(),'Granola')]")));
            System.out.println("Granola subcategory was opened.");

            // Granola başlığı altındaki  ürünleri al
            WebElement granolaHeader = driver.findElement(By.xpath("//h5[@data-testid='title' and text()='Granola']"));
            WebElement granolaCardWrapper = granolaHeader.findElement(
                    By.xpath("./ancestor::div[contains(@class, 'style__HeaderWrapper')]/following-sibling::div"));
            List<WebElement> granolaProducts = granolaCardWrapper.findElements(By.cssSelector("article.sc-c016d6c1-0"));

            double lowestPrice = Double.MAX_VALUE;
            List<WebElement> lowestPriceProducts = new ArrayList<>();

            for (WebElement product : granolaProducts) {
                try {
                    WebElement priceElement = product.findElement(By.cssSelector("div.sc-c016d6c1-5 span[data-testid='text']"));
                    String priceText = priceElement.getText().replace("₺", "").replace(",", ".").trim();
                    double price = Double.parseDouble(priceText);

                    if (price < lowestPrice) {
                        lowestPrice = price;
                        lowestPriceProducts.clear(); // daha ucuzu varmış, listeyi sıfırla
                        lowestPriceProducts.add(product); // aynı fiyatta başka ürünü listeye ekledik
                    } else if (price == lowestPrice) {
                        lowestPriceProducts.add(product);
                    }
                } catch (Exception e) {
                    System.out.println("Could not get product price: " + e.getMessage());
                }
            }

            if (lowestPriceProducts.isEmpty()) {
                System.out.println("The lowest price product in the Granola subcategory could not be found.");
                return;
            }

            System.out.println("Lowest price in the Granola subcategory: ₺" + lowestPrice);
            System.out.println("Total " + lowestPriceProducts.size() + " product found at this price. ");

            // Listedeki ilk ürüne tıkla
            WebElement selectedProduct = lowestPriceProducts.get(0);
            WebElement productLink = selectedProduct.findElement(By.cssSelector("a[href]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", productLink);
            Thread.sleep(1000);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", productLink);
            System.out.println("Redirected to the lowest priced granola product detail page.");

            // Ürün detay sayfasındaki fiyatı al ve karşılaştır
            WebDriverWait detailWait = new WebDriverWait(driver, Duration.ofSeconds(20));
            WebElement detailPriceElement = detailWait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.sc-4e247a28-7 span")));
            String detailPriceText = detailPriceElement.getText().replace("₺", "").replace(",", ".").trim();
            double detailPrice = Double.parseDouble(detailPriceText);

            if (Double.compare(detailPrice, lowestPrice) == 0) {
                System.out.println("Test Passed: The product price on the category page appears to be the same as the price in the product details. ₺" + detailPrice);
            } else {
                System.out.println("Test Failed: These price are different! Category: ₺" + lowestPrice + ", Product detail: ₺" + detailPrice);
            }

        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Tarayıcıyı kapat
            driver.quit();
        }
    }
}