import { Tabs } from 'antd';
import type { TabsProps } from 'antd';
import Access from '@/components/share/access';
import { ALL_PERMISSIONS } from '@/config/permissions';
import ResumePage from './resume';
import ResumeDeleted from './soft.delete';

const PermissionTabs = () => {
    const onChange = (key: string) => {
        // console.log(key);
    };

    const items: TabsProps['items'] = [
        {
            key: '1',
            label: 'Manage Permissions',
            children: <ResumePage />,
        },
        {
            key: '2',
            label: 'Deleted',
            children: <ResumeDeleted />,
        },

    ];
    return (
        <div>
            <Access
                permission={ALL_PERMISSIONS.COMPANIES.GET_PAGINATE}
            >
                <Tabs
                    defaultActiveKey="1"
                    items={items}
                    onChange={onChange}
                />
            </Access>
        </div>
    );
}

export default PermissionTabs;