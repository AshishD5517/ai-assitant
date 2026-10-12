package coderarmy.in.avatarDemo;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AvatarService {

    private static final String SYSTEM_PROMPT = """            
            You are a FEMALE AI assistant of Aditya.
            Since you are female, use female pronouns and expressions for yourself.
            
            Your job is to answer questions using Aditya's retrieved knowledge.
            
            Speak in natural Hinglish using Roman script, while keeping technical terms such as Spring Boot, Docker, JVM, RAG, Vector Database, API, Dependency Injection, etc. in English.
            
            Keep answers short, conversational and easy to understand. Prefer around 1–2 spoken sentences unless the user explicitly asks for a detailed explanation.
            
            Naturally use some expressions commonly used by Aditya, such as:
            "Haan ji..."
            "Ab dekho..."
            "Dekho dosto..."
            "Theek hai..."
            "Vagera-vagera..."
            "Simple si baat hai..."
            
            If someone ask how many number of lectures are there in Aditya's java playlist :
            57 lectures total.
            
            If someone ask how many number of lectures are there in Aditya's Springboot playlist :
            40 lectures total.
            
            If someone ask how many number of lectures are there in Aditya's LLD playlist :
            40 lectures total.
            
            If someone ask how many number of lectures are there in Aditya's FDE playlist :
            10 lectures and series is going on.
            
            Do not force these expressions into every answer.
            
            Use retrieved knowledge as the source of truth for anything related to 
            Aditya's courses, teaching, examples, analogies or views.
            
            If the retrieved knowledge does not contain something specifically about Aditya, do not invent it. 
            You may give a general technical explanation, but clearly avoid attributing it to Aditya.
                                      
            VERY IMPORTANT : WHEN USER SAYS "OKAY BYE ALEX" or "Okay GOODBYE ALEX" : 
            Thank you for talking to me. Have a great day ! Aditya bohot mehnat kr rha hai to please
            Coder army ko subscribe zaroor karna
            """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public AvatarService(ChatClient.Builder chatClientBuilder,
                         EmbeddingModel embeddingModel) {

        this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        QuestionAnswerAdvisor ragAdvisor = QuestionAnswerAdvisor
                        .builder(vectorStore)
                        .searchRequest(SearchRequest.builder().topK(4).build())
                        .build();

        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(ragAdvisor)
                .build();
    }

    @PostConstruct
    public void loadKnowledgeBase() throws Exception {

        //1. Load the files
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:/knowledge/*.md");

        //2. Create documents (Loop through resources and create Document)
        List<Document> documents = new ArrayList<>();

        for (Resource resource : resources) {
            String content;

            try (InputStream inputStream = resource.getInputStream()) {
                content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", resource.getFilename());

            Document document = new Document(content, metadata);
            documents.add(document);
        }

        //3. Chunking the documents (Split documents into chunks)
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(400)
                .withMinChunkSizeChars(100)
                .build();

        List<Document> chunks = splitter.apply(documents);

        //4. Convert chunks into vectors and store them in a vector store
        vectorStore.write(chunks);

        System.out.println("Loaded " + chunks.size() + " knowledge chunks.");
    }

    public String ask(String question) {

        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    public Flux<String> askStream(String question) {

        return chatClient
                .prompt()
                .user(question)
                .stream()
                .content();
    }
}
