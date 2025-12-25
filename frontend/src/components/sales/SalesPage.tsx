/**
 * Sales Automation Main Page
 * Container component with tab navigation
 */

import { useState } from 'react';
import {
  Box,
  Tabs,
  Tab,
  Typography,
  Container,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  People as PeopleIcon,
  ViewKanban as KanbanIcon,
  Email as EmailIcon,
  Business as BusinessIcon,
} from '@mui/icons-material';
import { SalesDashboard } from './SalesDashboard';
import { LeadList } from './LeadList';
import { PipelineBoard } from './PipelineBoard';
import { SequenceList } from './SequenceList';
import { AccountList } from './AccountList';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`sales-tabpanel-${index}`}
      aria-labelledby={`sales-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

function a11yProps(index: number) {
  return {
    id: `sales-tab-${index}`,
    'aria-controls': `sales-tabpanel-${index}`,
  };
}

export function SalesPage() {
  const [activeTab, setActiveTab] = useState(0);
  const tenantId = 'TENANT001'; // Default tenant for testing

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  return (
    <Container maxWidth="xl" sx={{ py: 2 }}>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Sales Automation
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Manage leads, pipeline, and email sequences
        </Typography>
      </Box>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
        <Tabs
          value={activeTab}
          onChange={handleTabChange}
          aria-label="sales navigation tabs"
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab
            icon={<DashboardIcon />}
            iconPosition="start"
            label="Dashboard"
            {...a11yProps(0)}
          />
          <Tab
            icon={<PeopleIcon />}
            iconPosition="start"
            label="Leads"
            {...a11yProps(1)}
          />
          <Tab
            icon={<KanbanIcon />}
            iconPosition="start"
            label="Pipeline"
            {...a11yProps(2)}
          />
          <Tab
            icon={<BusinessIcon />}
            iconPosition="start"
            label="Accounts"
            {...a11yProps(3)}
          />
          <Tab
            icon={<EmailIcon />}
            iconPosition="start"
            label="Sequences"
            {...a11yProps(4)}
          />
        </Tabs>
      </Box>

      <TabPanel value={activeTab} index={0}>
        <SalesDashboard tenantId={tenantId} />
      </TabPanel>

      <TabPanel value={activeTab} index={1}>
        <LeadList tenantId={tenantId} />
      </TabPanel>

      <TabPanel value={activeTab} index={2}>
        <PipelineBoard tenantId={tenantId} />
      </TabPanel>

      <TabPanel value={activeTab} index={3}>
        <AccountList tenantId={tenantId} />
      </TabPanel>

      <TabPanel value={activeTab} index={4}>
        <SequenceList tenantId={tenantId} />
      </TabPanel>
    </Container>
  );
}

export default SalesPage;
