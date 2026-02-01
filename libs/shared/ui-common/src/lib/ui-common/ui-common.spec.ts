import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiCommon } from './ui-common';

describe('UiCommon', () => {
  let component: UiCommon;
  let fixture: ComponentFixture<UiCommon>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiCommon],
    }).compileComponents();

    fixture = TestBed.createComponent(UiCommon);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
