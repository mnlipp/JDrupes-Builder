package org.jdrupes.builder.mvnrepo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.repository.RemoteRepository;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MavenContextTests {

    @Test
    void testRepositories() {
        String profileId = "test-profile";

        // Case 1: Profile not active
        Settings s1 = new Settings();
        assertTrue(MavenContext.repositories(s1, profileId)
            .collect(Collectors.toList()).isEmpty());

        // Case 2: Profile active but not present in profiles map
        Settings s2 = new Settings();
        s2.setActiveProfiles(List.of(profileId));
        assertTrue(MavenContext.repositories(s2, profileId)
            .collect(Collectors.toList()).isEmpty());

        // Case 3: Profile active and present with repositories
        Profile profile = new Profile();
        profile.setId(profileId);
        Repository repo = new Repository();
        repo.setId("test-repo");
        repo.setUrl("https://test.repo/maven2");
        RepositoryPolicy releasePolicy = new RepositoryPolicy();
        releasePolicy.setEnabled(true);
        repo.setReleases(releasePolicy);
        RepositoryPolicy snapshotPolicy = new RepositoryPolicy();
        snapshotPolicy.setEnabled(false);
        repo.setSnapshots(snapshotPolicy);
        profile.addRepository(repo);
        List<Profile> profiles = new ArrayList<>();
        profiles.add(profile);
        Settings s3 = new Settings();
        s3.setProfiles(profiles);
        s3.setActiveProfiles(List.of(profileId));

        List<RemoteRepository> result = MavenContext
            .repositories(s3, profileId).collect(Collectors.toList());
        assertEquals(1, result.size());
        RemoteRepository remoteRepo = result.get(0);
        assertEquals("test-repo", remoteRepo.getId());
        assertEquals("https://test.repo/maven2", remoteRepo.getUrl());
    }
}
