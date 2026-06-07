import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ResumeBuilderService } from '../../services/resume-builder.service';
import {
  ResumeCustomSection,
  ResumeCustomSectionItem,
  ResumeCustomSectionPlacement,
  ResumeEducation,
  ResumeExperience,
  ResumeProject,
  ResumeRequest,
  ResumeTemplate
} from '../../models/buildermodel';
import { SeoService } from '../../../seo/seo.service';

type ResumeRequestKey = keyof ResumeRequest;
type ResumeNotice = { type: 'success' | 'error' | 'info'; message: string };
type CustomSectionPreset = { title: string; placement: ResumeCustomSectionPlacement; subtitle: string };

@Component({
  selector: 'app-resume-builder',
  imports: [CommonModule, FormsModule],
  templateUrl: './resume-builder.component.html',
  styleUrl: './resume-builder.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResumeBuilderComponent {
  private readonly resumeBuilderService = inject(ResumeBuilderService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly seoService = inject(SeoService);

  templates = signal<ResumeTemplate[]>([]);
  selectedTemplate = signal('classic');
  previewHtml = signal<SafeHtml | null>(null);
  notice = signal<ResumeNotice | null>(null);
  isPreviewing = signal(false);
  isDownloading = signal(false);
  hasPreview = computed(() => this.previewHtml() !== null);
  customSectionPresets: CustomSectionPreset[] = [
    { title: 'Certifications', placement: 'sidebar', subtitle: 'Issuer or credential details' },
    { title: 'Awards', placement: 'main', subtitle: 'Awarding body or year' },
    { title: 'Languages', placement: 'sidebar', subtitle: 'Proficiency or context' },
    { title: 'Volunteering', placement: 'main', subtitle: 'Organization or dates' },
    { title: 'Publications', placement: 'main', subtitle: 'Publisher, venue, or link' },
    { title: 'Others', placement: 'main', subtitle: 'Any subtitle' }

  ];

  resume: ResumeRequest = {
    fullName: 'John Doe',
    title: 'Angular and Spring Boot Developer',
    email: 'john@example.com',
    phone: '9876543210',
    location: 'Bangalore, India',
    summary: 'Full-stack developer with 4+ years of experience building reliable Angular applications, Spring Boot APIs, and document automation workflows. Strong focus on performance, clean UI implementation, and shipping production-ready features across the full product lifecycle.',
    photoDataUri: '',
    skills: ['Angular', 'TypeScript', 'Spring Boot', 'Java', 'PostgreSQL', 'REST APIs', 'AWS S3', 'Docker', 'RxJS', 'Material UI'],
    experience: [
      {
        role: 'Senior Software Developer',
        company: 'NexaCloud Labs',
        location: 'Bangalore',
        startDate: 'Jan 2024',
        endDate: '',
        current: true,
        points: [
          'Led delivery of Angular dashboards and Spring Boot APIs used by 20,000+ monthly active users.',
          'Reduced average document processing time by 32% through optimized upload flows, async job handling, and API response tuning.',
          'Mentored 3 junior engineers and introduced reusable UI patterns that improved feature delivery consistency.'
        ]
      },
      {
        role: 'Software Developer',
        company: 'ABC Company',
        location: 'Bangalore',
        startDate: 'Jul 2021',
        endDate: 'Dec 2023',
        current: false,
        points: [
          'Built REST APIs using Spring Boot, PostgreSQL, and JWT-based authentication for customer-facing products.',
          'Created responsive Angular screens for document workflows, account management, and reporting modules.',
          'Improved frontend build quality by adding validation utilities, shared services, and cleaner error handling.'
        ]
      },
      {
        role: 'Frontend Engineer Intern',
        company: 'BrightStack Digital',
        location: 'Remote',
        startDate: 'Jan 2021',
        endDate: 'Jun 2021',
        current: false,
        points: [
          'Converted product requirements into accessible Angular components with responsive layouts.',
          'Collaborated with designers to polish forms, empty states, and loading interactions for a SaaS onboarding flow.'
        ]
      }
    ],
    education: [
      {
        degree: 'B.Tech Computer Science',
        institution: 'ABC University',
        location: 'Bangalore',
        duration: '2019 - 2023',
        details: 'Focused on web applications, databases, and software engineering.'
      }
    ],
    projects: [
      {
        name: 'Document Utility Platform',
        description: 'A web application for converting, editing, and managing PDF workflows.',
        url: '',
        points: [
          'Implemented upload, preview, and download flows for document tools.',
          'Added reusable validation and thumbnail components to support PDF and image workflows.'
        ]
      },
      {
        name: 'AI Knowledge Assistant',
        description: 'A RAG-based PDF chat experience with document ingestion, embeddings, and WebSocket progress updates.',
        url: '',
        points: [
          'Built the chat interface, document status panel, and real-time ingestion event handling.',
          'Integrated Spring Boot endpoints for querying document chunks and returning source-aware answers.'
        ]
      },
      {
        name: 'Expense Insights Dashboard',
        description: 'Analytics dashboard for tracking team budgets, invoice status, and monthly spend trends.',
        url: '',
        points: [
          'Designed filterable charts, summary cards, and export-ready tables in Angular.',
          'Created backend reporting APIs with pagination, sorting, and date-range filtering.'
        ]
      }
    ],
    links: [
      { label: 'Portfolio', url: 'https://example.com' },
      { label: 'LinkedIn', url: 'https://linkedin.com/in/johndoe' },
      { label: 'GitHub', url: 'https://github.com/johndoe' }
    ],
    customSections: [
      {
        title: 'Certifications',
        placement: 'sidebar',
        items: [
          {
            title: 'AWS Certified Cloud Practitioner',
            subtitle: 'Amazon Web Services',
            points: []
          },
          {
            title: 'Angular Developer Certification',
            subtitle: 'Professional frontend track',
            points: []
          }
        ]
      },
      {
        title: 'Achievements',
        placement: 'main',
        items: [
          {
            title: 'Quarterly Engineering Excellence Award',
            subtitle: 'NexaCloud Labs, 2025',
            points: [
              'Recognized for improving release quality and reducing production support tickets across document workflows.'
            ]
          }
        ]
      }
    ]
  };

  ngOnInit() {
    this.seoService.applySEO('resume-builder');
    this.loadTemplates();
    this.preview();
  }

  loadTemplates() {
    this.resumeBuilderService.getTemplates().subscribe({
      next: response => {
        this.templates.set(response.data ?? []);
        if (!this.templates().some(template => template.id === this.selectedTemplate())) {
          this.selectedTemplate.set(this.templates()[0]?.id ?? 'classic');
        }
      }
    });
  }

  selectTemplate(templateId: string) {
    this.selectedTemplate.set(templateId);
    this.preview();
  }

  preview() {
    const request = this.buildRequest();

    if (!this.validateRequiredFields(request)) {
      return;
    }

    this.isPreviewing.set(true);
    this.resumeBuilderService.previewResume(request, this.selectedTemplate()).subscribe({
      next: html => {
        this.previewHtml.set(this.sanitizer.bypassSecurityTrustHtml(html));
        this.showNotice('success', 'Preview updated.');
        this.isPreviewing.set(false);
      },
      error: () => {
        this.isPreviewing.set(false);
      }
    });
  }

  download() {
    const request = this.buildRequest();

    if (!this.validateRequiredFields(request)) {
      return;
    }

    this.isDownloading.set(true);
    this.resumeBuilderService.downloadResume(request, this.selectedTemplate()).subscribe({
      next: response => {
        this.isDownloading.set(false);
        this.downloadBlob(response);
        this.showNotice('success', 'Resume downloaded.');
      },
      error: () => {
        this.isDownloading.set(false);
      }
    });
  }

  addSkill() {
    this.resume.skills.push('');
  }

  removeSkill(index: number) {
    this.resume.skills.splice(index, 1);
  }

  addExperience() {
    this.resume.experience.push(this.createExperience());
  }

  removeExperience(index: number) {
    this.resume.experience.splice(index, 1);
  }

  addExperiencePoint(experience: ResumeExperience) {
    experience.points.push('');
  }

  removeExperiencePoint(experience: ResumeExperience, index: number) {
    experience.points.splice(index, 1);
  }

  addEducation() {
    this.resume.education.push(this.createEducation());
  }

  removeEducation(index: number) {
    this.resume.education.splice(index, 1);
  }

  addProject() {
    this.resume.projects.push(this.createProject());
  }

  removeProject(index: number) {
    this.resume.projects.splice(index, 1);
  }

  addProjectPoint(project: ResumeProject) {
    project.points.push('');
  }

  removeProjectPoint(project: ResumeProject, index: number) {
    project.points.splice(index, 1);
  }

  addLink() {
    this.resume.links.push({ label: '', url: '' });
  }

  removeLink(index: number) {
    this.resume.links.splice(index, 1);
  }

  addCustomSection(preset?: CustomSectionPreset) {
    this.resume.customSections.push(this.createCustomSection(preset));
  }

  removeCustomSection(index: number) {
    this.resume.customSections.splice(index, 1);
  }

  addCustomSectionItem(section: ResumeCustomSection) {
    section.items.push(this.createCustomSectionItem());
  }

  removeCustomSectionItem(section: ResumeCustomSection, index: number) {
    section.items.splice(index, 1);
  }

  addCustomSectionPoint(item: ResumeCustomSectionItem) {
    item.points.push('');
  }

  removeCustomSectionPoint(item: ResumeCustomSectionItem, index: number) {
    item.points.splice(index, 1);
  }

  onCurrentRoleChange(experience: ResumeExperience) {
    if (experience.current) {
      experience.endDate = '';
    }
  }

  onPhotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.showNotice('error', 'Use a PNG or JPEG photo.');
      input.value = '';
      return;
    }

    if (file.size > 1_500_000) {
      this.showNotice('error', 'Use a photo under 1.5 MB.');
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.resume.photoDataUri = reader.result as string;
      this.preview();
    };
    reader.readAsDataURL(file);
  }

  removePhoto() {
    this.resume.photoDataUri = '';
    this.preview();
  }

  private buildRequest(): ResumeRequest {
    return {
      ...this.resume,
      fullName: this.trim(this.resume.fullName),
      title: this.trim(this.resume.title),
      email: this.trim(this.resume.email),
      phone: this.trim(this.resume.phone),
      location: this.trim(this.resume.location),
      summary: this.trim(this.resume.summary),
      skills: this.cleanStrings(this.resume.skills),
      experience: this.resume.experience.map(item => ({
        ...item,
        role: this.trim(item.role),
        company: this.trim(item.company),
        location: this.trim(item.location),
        startDate: this.trim(item.startDate),
        endDate: this.trim(item.endDate),
        points: this.cleanStrings(item.points)
      })).filter(item => this.hasValues(item, ['role', 'company', 'location', 'startDate', 'endDate']) || item.points.length > 0),
      education: this.resume.education.map(item => ({
        ...item,
        degree: this.trim(item.degree),
        institution: this.trim(item.institution),
        location: this.trim(item.location),
        duration: this.trim(item.duration),
        details: this.trim(item.details)
      })).filter(item => this.hasValues(item, ['degree', 'institution', 'location', 'duration', 'details'])),
      projects: this.resume.projects.map(item => ({
        ...item,
        name: this.trim(item.name),
        description: this.trim(item.description),
        url: this.trim(item.url),
        points: this.cleanStrings(item.points)
      })).filter(item => this.hasValues(item, ['name', 'description', 'url']) || item.points.length > 0),
      links: this.resume.links.map(item => ({
        label: this.trim(item.label),
        url: this.trim(item.url)
      })).filter(item => !!item.url),
      customSections: this.resume.customSections.map(section => ({
        title: this.trim(section.title),
        placement: this.resolveCustomSectionPlacement(section.placement),
        items: section.items.map(item => ({
          title: this.trim(item.title),
          subtitle: this.trim(item.subtitle),
          points: this.cleanStrings(item.points)
        })).filter(item => this.hasValues(item, ['title', 'subtitle']) || item.points.length > 0)
      })).filter(section => !!section.title || section.items.length > 0)
    };
  }

  private validateRequiredFields(request: ResumeRequest): boolean {
    if (!request.fullName || !request.email) {
      this.showNotice('error', 'Full name and email are required.');
      return false;
    }

    const hasResumeSection = !!request.summary
      || request.skills.length > 0
      || request.experience.length > 0
      || request.education.length > 0
      || request.projects.length > 0
      || request.customSections.length > 0;

    if (!hasResumeSection) {
      this.showNotice('error', 'Add at least one resume section before previewing.');
      return false;
    }

    return true;
  }

  private showNotice(type: ResumeNotice['type'], message: string) {
    this.notice.set({ type, message });
  }

  private downloadBlob(response: { body: Blob | null; headers: { get(name: string): string | null } }) {
    const blob = response.body;
    if (!blob) {
      return;
    }

    const contentDisposition = response.headers.get('content-disposition');
    const match = contentDisposition?.match(/filename="([^"]+)"/);
    const fileName = match?.[1] ?? 'resume.pdf';
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');

    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  private cleanStrings(values: string[]): string[] {
    return values.map(value => this.trim(value)).filter(Boolean);
  }

  private trim(value: string | null | undefined): string {
    return value?.trim() ?? '';
  }

  private hasValues<T extends Record<string, unknown>>(item: T, keys: ResumeRequestKey[] | string[]): boolean {
    return keys.some(key => {
      const value = item[key];
      return typeof value === 'string' && value.trim().length > 0;
    });
  }

  private createExperience(): ResumeExperience {
    return {
      role: '',
      company: '',
      location: '',
      startDate: '',
      endDate: '',
      current: false,
      points: ['']
    };
  }

  private createEducation(): ResumeEducation {
    return {
      degree: '',
      institution: '',
      location: '',
      duration: '',
      details: ''
    };
  }

  private createProject(): ResumeProject {
    return {
      name: '',
      description: '',
      url: '',
      points: ['']
    };
  }

  private createCustomSection(preset?: CustomSectionPreset): ResumeCustomSection {
    return {
      title: preset?.title ?? '',
      placement: this.resolveCustomSectionPlacement(preset?.placement),
      items: [
        {
          title: '',
          subtitle: preset?.subtitle ?? '',
          points: ['']
        }
      ]
    };
  }

  private createCustomSectionItem(): ResumeCustomSectionItem {
    return {
      title: '',
      subtitle: '',
      points: ['']
    };
  }

  private resolveCustomSectionPlacement(placement: string | null | undefined): ResumeCustomSectionPlacement {
    return placement === 'main' ? 'main' : 'sidebar';
  }

  ngOnDestroy() {
    this.seoService.cleanup();
  }
}
