package br.com.me.apileitura.summary;

import br.com.me.apileitura.infra.cookie.Cookies;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;

@RestController
class SummaryController {

    private final SummaryRepository summaryRepository;
    private final Cookies cookies;

    SummaryController(SummaryRepository summaryRepository, Cookies cookies) {
        this.summaryRepository = summaryRepository;
        this.cookies = cookies;
    }

    @PostMapping(value = "/api/summaries")
    public ResponseEntity createSummary(@RequestBody @Valid NewSummaryFormInputRequest newSummaryFormInputRequest){
        Summary summary = summaryRepository.save(newSummaryFormInputRequest.toModel());
        URI location = URI.create("/api/summaries/" + summary.getId());
        return created(location).build();
    }

    @GetMapping(value = "/api/summaries")
    public ResponseEntity listSummaries(){
        List<Summary> summaries = summaryRepository.findAll();
        List<SummariesWithFirstChapter> summariesWithFirstChapters = summaries.stream().map(SummariesWithFirstChapter::new).collect(Collectors.toList());
        return ok(summariesWithFirstChapters);
    }

    @GetMapping(value = "/api/summary/{id}/chapter")
    public ResponseEntity firstChapter(@PathVariable("id") Long id){
        Summary summary = summaryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Summary not found"));
        FirstSummary firstSummary = new FirstSummary(summary.getFirstChapter());
        return ok(firstSummary);
    }

    @PostMapping(value = "/api/summary/{id}/next-chapter")
    public ResponseEntity nextChapter(@PathVariable("id") Long id, Integer currentChapter, HttpServletResponse response){
        Summary summary = summaryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Summary not found"));
        Optional<Chapter> possibleChapter = SummaryStatus.NEXT.execute(summary.getChapters(), currentChapter);
        if(possibleChapter.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        Cookie cookie = cookies.createCookie("chapter", possibleChapter.get());
        response.addCookie(cookie);
        return ok(possibleChapter.get());
    }

    @PostMapping(value = "/api/summary/{id}/previous-chapter")
    public ResponseEntity previousChapter(@PathVariable("id") Long id, Integer currentChapter, HttpServletResponse response){
        Summary summary = summaryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Summary not found"));
        Optional<Chapter> possibleChapter = SummaryStatus.PREVIOUS.execute(summary.getChapters(), currentChapter);
        if(possibleChapter.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        Cookie cookie = cookies.createCookie("chapter", possibleChapter.get());
        response.addCookie(cookie);
        return ok(possibleChapter.get());
    }

    @GetMapping(value = "/api/summary/{id}/current")
    public ResponseEntity currentSummary(@PathVariable("id") Long id, @CookieValue("chapter") Optional<String> currentChapter, HttpServletResponse response){
        Summary summary = summaryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Summary not found"));
        if(currentChapter.isEmpty()){
            Cookie cookie = cookies.createCookie("chapter", summary.getFirstChapter());
            response.addCookie(cookie);
            return ResponseEntity.ok(summary.getFirstChapter());
        }
        return ResponseEntity.ok(currentChapter.get());
    }

    @PostMapping(value = "/api/summary/{id}/finish")
    public ResponseEntity finishSummary(@PathVariable("id") Long id, Integer currentPage){
        Summary summary = summaryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Summary not found"));
        if(currentPage > summary.getChapters().size()){
            return ResponseEntity.ok(summary.getFirstChapter());
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cannot be finish summary");
    }

    @GetMapping("/all-cookies")
    public String readAllCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .map(c -> c.getName() + "=" + c.getValue()).collect(Collectors.joining(", "));
        }
        return "No cookies";
    }
}
