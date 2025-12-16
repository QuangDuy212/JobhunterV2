import { Tabs } from 'antd';
import type { TabsProps } from 'antd';
import Access from '@/components/share/access';
import { ALL_PERMISSIONS } from '@/config/permissions';
import RolePage from './role';
import RoleDeleted from './soft.delete';

const RoleTabs = () => {
    const onChange = (key: string) => {
        // console.log(key);
    };

    const items: TabsProps['items'] = [
        {
            key: '1',
            label: 'Manage Role',
            children: <RolePage />,
        },
        {
            key: '2',
            label: 'Deleted',
            children: <RoleDeleted />,
        },

    ];
    return (
        <div>
            <Access
                permission={ALL_PERMISSIONS.ROLES.GET_PAGINATE}
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

export default RoleTabs;