import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { PublishConfirmDialogComponent } from './publish-confirm-dialog.component';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('PublishConfirmDialogComponent', () => {
  let fixture: ComponentFixture<PublishConfirmDialogComponent>;
  let component: PublishConfirmDialogComponent;
  let dialogRef: { close: jest.Mock };

  beforeEach(async () => {
    dialogRef = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [PublishConfirmDialogComponent],
      providers: [{ provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { measureId: 'm1', measureName: 'Measure A', currentVersion: '2.4.7' },
        },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    fixture = TestBed.createComponent(PublishConfirmDialogComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('suggests the next version on init', () => {
    expect(component.newVersion).toBe('2.4.8');
  });

  it('requires valid version, notes, and checklist to publish', () => {
    component.newVersion = '1.0.0';
    component.releaseNotes = '';

    expect(component.canPublish()).toBe(false);

    component.releaseNotes = 'Changes';
    component.cqlTested = true;
    component.valueSetsVerified = true;
    component.documentationComplete = true;
    component.peerReviewed = true;

    expect(component.canPublish()).toBe(true);
  });

  it('publishes and closes dialog when requirements are met', () => {
    jest.useFakeTimers();
    component.releaseNotes = 'Ready';
    component.cqlTested = true;
    component.valueSetsVerified = true;
    component.documentationComplete = true;
    component.peerReviewed = true;

    component.publish();

    expect(component.publishing).toBe(true);

    jest.advanceTimersByTime(2000);

    expect(component.publishing).toBe(false);
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });
});
