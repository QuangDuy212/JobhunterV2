import { Tabs } from 'antd';
import type { TabsProps } from 'antd';
import Access from '@/components/share/access';
import { ALL_PERMISSIONS } from '@/config/permissions';
import UserPage from './user';
import UserDeleted from './soft.detete';

const UserTabs = () => {
    const onChange = (key: string) => {
    };

    const items: TabsProps['items'] = [
        {
            key: '1',
            label: 'Manage Users',
            children: <UserPage />,
        },
        {
            key: '2',
            label: 'Deleted',
            children: <UserDeleted />,
        },

    ];
    return (
        <div>
            <Access
                permission={ALL_PERMISSIONS.USERS.GET_PAGINATE}
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

export default UserTabs;