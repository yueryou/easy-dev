package tech.lin2j.idea.plugin.model;

import org.junit.jupiter.api.Test;
import tech.lin2j.idea.plugin.enums.AuthType;

import static org.junit.jupiter.api.Assertions.*;

public class CredentialTemplateTest {

    @Test
    public void should_create_template_with_required_fields() {
        CredentialTemplate template = new CredentialTemplate();
        template.setName("Dev Template");
        template.setUsername("devuser");
        template.setAuthType(AuthType.PASSWORD.getCode());

        assertEquals("Dev Template", template.getName());
        assertEquals("devuser", template.getUsername());
        assertEquals(AuthType.PASSWORD.getCode(), template.getAuthType());
    }

    @Test
    public void should_generate_unique_uid_when_ensure_uid_called() {
        CredentialTemplate template = new CredentialTemplate();
        assertNull(template.getUid());

        template.ensureUid();
        String uid = template.getUid();

        assertNotNull(uid);
        assertFalse(uid.isEmpty());
    }

    @Test
    public void should_not_overwrite_existing_uid_when_ensure_uid_called() {
        CredentialTemplate template = new CredentialTemplate();
        template.setUid("existing-uid");

        template.ensureUid();

        assertEquals("existing-uid", template.getUid());
    }

    @Test
    public void get_uid_should_not_have_side_effects() {
        CredentialTemplate template = new CredentialTemplate();
        assertNull(template.getUid());

        // Calling getUid() multiple times should still return null
        assertNull(template.getUid());
        assertNull(template.getUid());
    }
}