# Google Calendar Appointment Scheduling Setup

**Account**: aaron@mahoosuc.solutions (Google Workspace)
**Status**: Ready for configuration

---

## Quick Setup (5 minutes)

### Step 1: Create Appointment Schedule in Google Calendar

1. Go to [calendar.google.com](https://calendar.google.com)
2. Click the **Create** button (+ icon)
3. Select **Appointment schedule**
4. Configure your schedule:

   **Title**: "HDIM Demo - 30 Minute Call"

   **Duration**: 30 minutes (recommended)

   **Available hours**:
   - Monday-Friday: 9:00 AM - 5:00 PM ET
   - Or customize based on your availability

   **Buffer time**: 15 minutes between appointments

   **Booking window**: Allow bookings 1-14 days in advance

   **Description**:
   ```
   Thank you for scheduling a demo of HDIM, the FHIR-native platform for HEDIS excellence.

   During this call, we'll:
   - Walk through the HDIM platform
   - Discuss your quality program challenges
   - Show you potential ROI for your organization
   - Answer any questions about FHIR, CQL, and HEDIS

   A Google Meet link will be added automatically.

   Questions? Email aaron@mahoosuc.solutions
   ```

5. Click **Save**

### Step 2: Get Your Booking Link

1. After saving, click on your appointment schedule
2. Click **Open booking page**
3. Copy the URL (format: `https://calendar.google.com/calendar/appointments/schedules/...`)

### Step 3: Update the Landing Page

Edit the file `app/schedule/page.tsx` and replace the placeholder URL:

```tsx
// Line 19 - Replace this URL with your actual booking link
const GOOGLE_CALENDAR_BOOKING_URL = 'https://calendar.google.com/calendar/appointments/schedules/YOUR_SCHEDULE_ID?gv=true'
```

### Step 4: Deploy Changes

```bash
cd landing-page-v0
npm run build
npx vercel --prod
```

---

## Alternative: Use Cal.com (Free)

If you prefer a more customizable booking page:

1. Go to [cal.com](https://cal.com)
2. Sign up with your Google account
3. Create a new event type (30-min demo)
4. Connect your Google Calendar
5. Get your booking URL (format: `https://cal.com/YOUR_USERNAME/30min`)
6. Update the schedule page with the Cal.com URL

---

## Booking Flow

```
Visitor clicks "Schedule Demo" →
  Landing page /schedule →
    Form submission OR direct calendar booking →
      Event appears in aaron@mahoosuc.solutions calendar →
        Google Meet link auto-generated →
          Calendar invite sent to visitor
```

---

## Files Updated

| File | Change |
|------|--------|
| `app/schedule/page.tsx` | **NEW** - Booking page with form and calendar link |
| `app/page.tsx` | Changed "Schedule a Consultation" → `/schedule` |
| `components/PortalNav.tsx` | Changed "Contact Sales" → `/schedule` |
| `app/explorer/page.tsx` | Changed "Contact Sales" → `/schedule` |

---

## Configuration Options

### Booking URL Location

Update in `app/schedule/page.tsx`:

```tsx
const GOOGLE_CALENDAR_BOOKING_URL = 'YOUR_BOOKING_URL_HERE'
```

### Email Recipient

Update in `app/schedule/page.tsx`:

```tsx
<a href="mailto:aaron@mahoosuc.solutions" ...>
```

### Meeting Options

Modify in `app/schedule/page.tsx`:

```tsx
const meetingOptions = [
  {
    title: '30-Minute Demo',
    duration: '30 min',
    description: 'Quick overview...',
    recommended: true
  },
  // Add more options...
]
```

---

## Next Steps

1. [ ] Create appointment schedule in Google Calendar
2. [ ] Copy booking URL
3. [ ] Update `GOOGLE_CALENDAR_BOOKING_URL` in schedule page
4. [ ] Test booking flow
5. [ ] Deploy to production

---

## Support

For questions about calendar integration:
- Google Calendar Help: https://support.google.com/calendar
- Cal.com Docs: https://cal.com/docs

---

*Last Updated: December 30, 2025*
