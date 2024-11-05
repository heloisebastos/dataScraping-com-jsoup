package br.gov.mt.fapemat.datascraping;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;
import okhttp3.ResponseBody;
import retrofit2.Callback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@SpringBootApplication
public class DatascrapingApplication {

	interface PdfService {
        @GET
        Call<ResponseBody> downloadPdf(@Url String fileUrl);
    }

    public static void main(String[] args) {
        String pageUrl = "https://www.fapemat.mt.gov.br/servicos?c=2411155&e=67884178";
        System.out.println("Acessando a página: " + pageUrl);

        try {
            Document document = Jsoup.connect(pageUrl).get();
            System.out.println("Página carregada com sucesso!");

            Elements links = document.select("a"); 
            System.out.println("Encontrados " + links.size() + " links na página.");

              String directoryName = "projeto-de-pesquisa";
              Path directoryPath = Paths.get(directoryName);
  
              if (!Files.exists(directoryPath)) {
                  Files.createDirectory(directoryPath); 
                  System.out.println("Pasta criada: " + directoryName);
              } else {
                  removerArquivos(directoryName);
              }
    
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://www.fapemat.mt.gov.br/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();

            PdfService pdfService = retrofit.create(PdfService.class);

            for (Element link : links) {
                String linkText = link.text(); 
                String href = link.attr("abs:href"); 

                if (href.toLowerCase().contains("pdf")) {
                    System.out.println("Baixando PDF de: " + linkText);
                    baixarPdf(pdfService, href, directoryName); 
                } else {
                    System.out.println("Link ignorado: " + linkText);
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao acessar a página: " + e.getMessage());
        }
    }

    private static void removerArquivos(String directoryPath) {
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        System.out.println("Arquivo deletado: " + file.getName());
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            System.out.println("Erro no delay: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Falha ao deletar o arquivo: " + file.getName());
                    }
                }
            }
            System.out.println("Conteúdo da pasta " + directoryPath + " foi limpo.");
        } else {
            System.out.println("Pasta não encontrada: " + directoryPath);
        }
    }

    private static void baixarPdf(PdfService pdfService, String pdfUrl, String directory) {
        System.out.println("Iniciando o download do PDF: " + pdfUrl);
        pdfService.downloadPdf(pdfUrl).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String fileName = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1).replaceAll("%20", "_"); 
                        if (!fileName.toLowerCase().endsWith(".pdf")) {
                            fileName += ".pdf"; 
                        }
                        Path filePath = Paths.get(directory, fileName);
                        salvarPdf(response.body(), filePath.toString());
                        System.out.println("PDF baixado com sucesso: " + fileName);
                    } catch (IOException e) {
                        System.out.println("Erro ao salvar o PDF: " + e.getMessage());
                    }
                } else {
                    System.out.println("Erro ao baixar PDF: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                System.out.println("Falha no download do PDF: " + t.getMessage());
            }
        });
    }

	
    private static void salvarPdf(ResponseBody body, String filePath) throws IOException {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            inputStream = body.byteStream();
            File file = new File(filePath);
            fileOutputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("PDF salvo em: " + filePath);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }


}
