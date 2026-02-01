import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UtilAuth } from './util-auth';

describe('UtilAuth', () => {
  let component: UtilAuth;
  let fixture: ComponentFixture<UtilAuth>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UtilAuth],
    }).compileComponents();

    fixture = TestBed.createComponent(UtilAuth);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
