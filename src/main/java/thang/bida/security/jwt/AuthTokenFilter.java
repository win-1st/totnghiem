package thang.bida.security.jwt;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("========== AUTH TOKEN FILTER ==========");
        System.out.println("Method: " + request.getMethod());
        System.out.println("URI: " + request.getRequestURI());

        try {
            String jwt = parseJwt(request);
            System.out.println("JWT from request: "
                    + (jwt != null ? jwt.substring(0, Math.min(50, jwt.length())) + "..." : "null"));

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                System.out.println("Username from token: " + username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                System.out.println("UserDetails loaded: " + userDetails.getUsername());
                System.out.println("Authorities: " + userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("✅ Authentication set successfully for: " + username);
            } else {
                System.out.println("⚠️ JWT is null or invalid");
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
            System.out.println("❌ Authentication error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("========================================");
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        System.out.println("Authorization header: "
                + (headerAuth != null ? headerAuth.substring(0, Math.min(50, headerAuth.length())) + "..." : "null"));

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}