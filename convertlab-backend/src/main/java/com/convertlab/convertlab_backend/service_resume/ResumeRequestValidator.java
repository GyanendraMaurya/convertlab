package com.convertlab.convertlab_backend.service_resume;

import com.convertlab.convertlab_backend.service_resume.dto.ResumeEducationRequest;
import com.convertlab.convertlab_backend.service_resume.dto.ResumeExperienceRequest;
import com.convertlab.convertlab_backend.service_resume.dto.ResumeProjectRequest;
import com.convertlab.convertlab_backend.service_resume.dto.ResumeRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class ResumeRequestValidator {

    private static final int MAX_TEXT_LENGTH = 4000;
    private static final int MAX_PHOTO_DATA_URI_LENGTH = 2_800_000;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern IMAGE_DATA_URI_PATTERN = Pattern.compile("^data:image/(png|jpeg|jpg);base64,[A-Za-z0-9+/=\\r\\n]+$");

    public void validate(ResumeRequest resume) {
        if (resume == null) {
            throw new IllegalArgumentException("Resume data is required.");
        }

        requireText(resume.fullName(), "Full name is required.");
        requireText(resume.email(), "Email is required.");

        if (!EMAIL_PATTERN.matcher(resume.email().trim()).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }

        validateText(resume.fullName(), "Full name");
        validateText(resume.title(), "Title");
        validateText(resume.email(), "Email");
        validateText(resume.phone(), "Phone");
        validateText(resume.location(), "Location");
        validateText(resume.summary(), "Summary");

        validateStrings(resume.skills(), "Skill");
        validateExperience(resume.experience());
        validateEducation(resume.education());
        validateProjects(resume.projects());
        validatePhoto(resume.photoDataUri());

        if (!hasMeaningfulSection(resume)) {
            throw new IllegalArgumentException("Add at least one resume section such as summary, skills, experience, education, or projects.");
        }
    }

    private void validateExperience(List<ResumeExperienceRequest> experience) {
        if (experience == null) {
            return;
        }

        for (ResumeExperienceRequest item : experience) {
            if (item == null) {
                continue;
            }

            validateText(item.role(), "Experience role");
            validateText(item.company(), "Experience company");
            validateText(item.location(), "Experience location");
            validateText(item.startDate(), "Experience start date");
            validateText(item.endDate(), "Experience end date");
            validateStrings(item.points(), "Experience bullet");
        }
    }

    private void validateEducation(List<ResumeEducationRequest> education) {
        if (education == null) {
            return;
        }

        for (ResumeEducationRequest item : education) {
            if (item == null) {
                continue;
            }

            validateText(item.degree(), "Education degree");
            validateText(item.institution(), "Education institution");
            validateText(item.location(), "Education location");
            validateText(item.duration(), "Education duration");
            validateText(item.details(), "Education details");
        }
    }

    private void validateProjects(List<ResumeProjectRequest> projects) {
        if (projects == null) {
            return;
        }

        for (ResumeProjectRequest item : projects) {
            if (item == null) {
                continue;
            }

            validateText(item.name(), "Project name");
            validateText(item.description(), "Project description");
            validateText(item.url(), "Project URL");
            validateStrings(item.points(), "Project bullet");
        }
    }

    private void validateStrings(List<String> values, String label) {
        if (values == null) {
            return;
        }

        for (String value : values) {
            validateText(value, label);
        }
    }

    private void validatePhoto(String photoDataUri) {
        if (isBlank(photoDataUri)) {
            return;
        }

        if (photoDataUri.length() > MAX_PHOTO_DATA_URI_LENGTH) {
            throw new IllegalArgumentException("Candidate photo is too large.");
        }

        if (!IMAGE_DATA_URI_PATTERN.matcher(photoDataUri).matches()) {
            throw new IllegalArgumentException("Candidate photo must be a PNG or JPEG image.");
        }
    }

    private boolean hasMeaningfulSection(ResumeRequest resume) {
        return !isBlank(resume.summary())
                || hasText(resume.skills())
                || hasExperience(resume.experience())
                || hasEducation(resume.education())
                || hasProjects(resume.projects());
    }

    private boolean hasExperience(List<ResumeExperienceRequest> experience) {
        if (experience == null) {
            return false;
        }

        return experience.stream().anyMatch(item ->
                item != null && (!isBlank(item.role()) || !isBlank(item.company()) || hasText(item.points()))
        );
    }

    private boolean hasEducation(List<ResumeEducationRequest> education) {
        if (education == null) {
            return false;
        }

        return education.stream().anyMatch(item ->
                item != null && (!isBlank(item.degree()) || !isBlank(item.institution()) || !isBlank(item.details()))
        );
    }

    private boolean hasProjects(List<ResumeProjectRequest> projects) {
        if (projects == null) {
            return false;
        }

        return projects.stream().anyMatch(item ->
                item != null && (!isBlank(item.name()) || !isBlank(item.description()) || hasText(item.points()))
        );
    }

    private boolean hasText(List<String> values) {
        return values != null && values.stream().anyMatch(value -> !isBlank(value));
    }

    private void requireText(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateText(String value, String label) {
        if (value != null && value.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(label + " is too long.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
