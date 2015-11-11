package jetbrains.buildServer.issueTracker.github;

import jetbrains.buildServer.issueTracker.AbstractIssueProvider;
import jetbrains.buildServer.issueTracker.IssueFetcher;
import jetbrains.buildServer.issueTracker.IssueFetcherAuthenticator;
import jetbrains.buildServer.issueTracker.IssueProviderType;
import jetbrains.buildServer.issueTracker.github.auth.GitHubAuthenticator;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces;
import static jetbrains.buildServer.issueTracker.github.GitHubConstants.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class GitHubIssueProvider extends AbstractIssueProvider {

  private static final Pattern REPOSITORY_PATTERN = Pattern.compile("(.+)/(.+)");

  public GitHubIssueProvider(@NotNull IssueProviderType type,
                             @NotNull IssueFetcher fetcher) {
    super(type.getType(), fetcher);
  }

  @NotNull
  @Override
  protected IssueFetcherAuthenticator getAuthenticator() {
    return new GitHubAuthenticator(myProperties);
  }

  @Override
  public void setProperties(@NotNull Map<String, String> map) {
    super.setProperties(map);
    myHost = map.get(PARAM_REPOSITORY);
    myFetchHost = myHost;
  }

  @NotNull
  @Override
  protected Pattern compilePattern(@NotNull Map<String, String> properties) {
    final Pattern result = super.compilePattern(properties);
    ((GitHubIssueFetcher) myFetcher).setPattern(result);
    return result;
  }

  @NotNull
  @Override
  protected String extractId(@NotNull final String match) {
    Matcher m = myPattern.matcher(match);
    if (m.find() && m.groupCount() >= 1) {
      return m.group(1);
    } else {
      return super.extractId(match);
    }
  }

  @NotNull
  @Override
  public PropertiesProcessor getPropertiesProcessor() {
    return MY_PROCESSOR;
  }

  private static final PropertiesProcessor MY_PROCESSOR = new PropertiesProcessor() {
    public Collection<InvalidProperty> process(Map<String, String> map) {
      final List<InvalidProperty> result = new ArrayList<InvalidProperty>();

      if (checkNotEmptyParam(result, map, PARAM_AUTH_TYPE, "Authentication type must be specified")) {
        // we have auth type. check against it
        final String authTypeParam = map.get(PARAM_AUTH_TYPE);
        if (authTypeParam.equals(AUTH_LOGINPASSWORD)) {
          checkNotEmptyParam(result, map, PARAM_USERNAME, "Username must be specified");
          checkNotEmptyParam(result, map, PARAM_PASSWORD, "Password must be specified");
        } else if (authTypeParam.equals(AUTH_ACCESSTOKEN)) {
          checkNotEmptyParam(result, map, PARAM_ACCESS_TOKEN, "Access token must be specified");
        }
      }
      if (checkNotEmptyParam(result, map, PARAM_PATTERN, "Issue pattern must not be empty")) {
        try {
          String patternString = map.get(PARAM_PATTERN);
          //noinspection ResultOfMethodCallIgnored
          Pattern.compile(patternString);
        } catch (PatternSyntaxException e) {
          result.add(new InvalidProperty(PARAM_PATTERN, "Syntax of issue pattern is not correct"));
        }
      }

      if (checkNotEmptyParam(result, map, PARAM_REPOSITORY, "Repository must be specified")) {
        String repo = map.get(PARAM_REPOSITORY);
        final Matcher m = REPOSITORY_PATTERN.matcher(repo);
        if (!m.matches()) {
          result.add(new InvalidProperty(PARAM_REPOSITORY, "Repository must be in format 'owner/repository name'"));
        }
      }
      return result;
    }


    private boolean checkNotEmptyParam(@NotNull final Collection<InvalidProperty> invalid,
                                       @NotNull final Map<String, String> map,
                                       @NotNull final String propertyName,
                                       @NotNull final String errorMessage) {
      if (isEmptyOrSpaces(map.get(propertyName))) {
        invalid.add(new InvalidProperty(propertyName, errorMessage));
        return false;
      }
      return true;
    }
  };
}
